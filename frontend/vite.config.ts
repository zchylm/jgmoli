import { readFile, writeFile } from 'node:fs/promises'
import { resolve } from 'node:path'
import { pathToFileURL } from 'node:url'
import { build, defineConfig, loadEnv, type Plugin, type ResolvedConfig } from 'vite'
import react from '@vitejs/plugin-react'

function searchIndexing(): Plugin {
  let config: ResolvedConfig
  return {
    name: 'search-indexing',
    configResolved(resolved) { config = resolved },
    transformIndexHtml(html) {
      if (config.base !== '/payment-preview/') return html
      return html
        .replace('content="index, follow, max-image-preview:large"', 'content="noindex, follow"')
        .replace('<link rel="canonical" href="https://jgmoli.com.au/" />', '')
        .replace(/<script type="application\/ld\+json">[\s\S]*?<\/script>/, '')
    },
    async closeBundle() {
      if (config.command !== 'build' || config.build.ssr || config.base !== '/') return
      const output = resolve(config.root, config.build.outDir, 'index.html')
      const serverOutput = resolve(config.cacheDir, 'prerender')
      await build({
        configFile: config.configFile,
        mode: config.mode,
        publicDir: false,
        logLevel: 'warn',
        build: {
          ssr: 'src/entry-server.tsx',
          outDir: serverOutput,
          emptyOutDir: true,
          rollupOptions: { output: { entryFileNames: 'entry-server.mjs' } },
        },
      })
      const { render } = await import(pathToFileURL(resolve(serverOutput, 'entry-server.mjs')).href)
      const html = await readFile(output, 'utf8')
      await writeFile(output, html.replace('<div id="root"></div>', () => `<div id="root">${render()}</div>`))
    },
  }
}

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')

  return {
    base: env.VITE_BASE_PATH || '/',
    plugins: [react(), searchIndexing()],
    server: {
      proxy: {
        '/api': env.VITE_DEV_API_TARGET || 'http://localhost:8080',
      },
    },
  }
})
