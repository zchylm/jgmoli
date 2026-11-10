import assert from 'node:assert/strict'
import { existsSync, readFileSync } from 'node:fs'
import { resolve } from 'node:path'

const home = 'https://jgmoli.com.au/'
const html = readFileSync('dist/index.html', 'utf8')
const body = html.split('<body>')[1]
assert(html.includes(`<link rel="canonical" href="${home}"`))
assert(html.includes('content="index, follow, max-image-preview:large"'))
assert(!html.includes('noindex'))
assert(html.includes('<title>JG MOLI Australia |'))
assert(!body.includes('<div id="root"></div>'), 'Homepage must contain pre-rendered content')
for (const name of ['MOLI Cockpit', 'MOLI Racer', 'JG MOLI Arena']) {
  assert(body.includes(name), `Missing visible product line: ${name}`)
}
for (const match of html.matchAll(/(?:src|href)="(\/assets\/[^"?#]+)"/g)) {
  assert(existsSync(resolve('dist', `.${match[1]}`)), `Missing built asset: ${match[1]}`)
}
const graph = JSON.parse(html.match(/<script type="application\/ld\+json">([\s\S]*?)<\/script>/)[1])['@graph']
const organization = graph.find(item => item['@type'] === 'Organization')
const website = graph.find(item => item['@type'] === 'WebSite')
assert.equal(organization.url, home)
assert.equal(organization.alternateName, 'JGMOLI')
assert.equal(website.publisher['@id'], organization['@id'])
assert(!graph.some(item => item['@type'] === 'Offer'), 'Do not publish provisional product offers')
const robots = readFileSync('dist/robots.txt', 'utf8')
assert(robots.includes('Allow: /'))
assert(!/^Disallow:\s*\/\s*$/m.test(robots))
assert(robots.includes(`Sitemap: ${home}sitemap.xml`))
const sitemap = readFileSync('dist/sitemap.xml', 'utf8')
assert.deepEqual([...sitemap.matchAll(/<loc>(.*?)<\/loc>/g)].map(match => match[1]), [home])
const preview = readFileSync('dist/payment-preview/index.html', 'utf8')
assert(preview.includes('content="noindex, follow"'), 'Payment preview must not be indexed')
assert(!preview.includes('rel="canonical"'))
assert(!preview.includes('application/ld+json'))
console.log('SEO checks passed: pre-rendered homepage, metadata, assets, sitemap and noindex preview.')
