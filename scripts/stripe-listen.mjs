import fs from 'node:fs'
import { spawn } from 'node:child_process'

const file = new URL('../backend/src/main/resources/application-local-private.properties', import.meta.url)
const config = fs.readFileSync(file, 'utf8')
const key = config.split(/\r?\n/).find(line => line.startsWith('jgmoli.stripe.secret-key='))?.slice('jgmoli.stripe.secret-key='.length).trim()
if (!key?.startsWith('sk_test_')) throw new Error('Configure a Stripe test secret key in the local private properties file.')

const child = spawn('stripe', ['listen', '--skip-update', '--events',
  'checkout.session.completed,checkout.session.async_payment_succeeded,checkout.session.async_payment_failed,checkout.session.expired',
  '--forward-to', 'http://localhost:8080/api/payments/stripe/webhook'], {
  env: { ...process.env, STRIPE_API_KEY: key },
})
let ready = false
let pending = ''
function consume(chunk) {
  pending += chunk.toString()
  const lines = pending.split(/[\r\n]/)
  pending = lines.pop() ?? ''
  for (const line of lines) {
    const secret = line.match(/whsec_[A-Za-z0-9]+/)
    if (secret && !ready) {
      let current = fs.readFileSync(file, 'utf8')
      const property = 'jgmoli.stripe.webhook-secret='
      current = current.split(/\r?\n/).filter(value => !value.startsWith(property)).join('\n')
      fs.writeFileSync(file, current.trimEnd() + '\n' + property + secret[0] + '\n', { mode: 0o600 })
      ready = true
      console.log('Stripe test listener ready. Signing secret saved locally. Restart the backend to load it.')
    } else if (!/secret|key|token/i.test(line)) console.log(line)
  }
}
child.stdout.on('data', consume)
child.stderr.on('data', consume)
child.on('error', () => console.error('Could not start Stripe CLI. Check that it is installed.'))
child.on('exit', code => { process.exitCode = code ?? 1 })
for (const signal of ['SIGINT', 'SIGTERM']) process.on(signal, () => child.kill(signal))
