# Google Search setup

The public homepage is `https://jgmoli.com.au/`. HTTP and www currently redirect to this address. The site is hosted on GitHub Pages.

## Website configuration

- The production build pre-renders the actual homepage, using its existing React components and image assets. Search engines can read the brand and three product lines before JavaScript execution.
- The homepage has an Australian title/description, a canonical URL, and Organization/WebSite structured data identifying JG MOLI and JGMOLI as the same brand. Provisional prices are not published as structured offers.
- `robots.txt` permits crawling and advertises `https://jgmoli.com.au/sitemap.xml`.
- The sitemap includes only the canonical homepage. Existing section anchors and hash-based shopping/account screens are not separate indexable pages.
- `/payment-preview/` has a static `noindex, follow` meta tag and is omitted from the sitemap. Crawling remains permitted so Google can see the noindex directive. Query-based account/payment entries and hash-based application routes also receive noindex in the browser.
- `npm run check:seo` validates the combined production/preview build in the deployment workflow.

## Owner action in Google Search Console

The owner will manage Search Console directly. No Google account access or additional permissions are required by the developer.

1. Open <https://search.google.com/search-console> using the owner's Google account.
2. Add a property. A Domain property for `jgmoli.com.au` requires adding Google's supplied TXT record at the DNS provider. Alternatively, choose a URL-prefix property for `https://jgmoli.com.au/` and select the HTML tag verification method.
3. For HTML tag verification, provide only the `google-site-verification` tag/code to the developer so it can be added to the homepage and deployed. This verification tag is public; do not provide passwords, cookies or access tokens. Return to Search Console and click Verify after deployment. Keep the verification record/tag in place.
4. Under Sitemaps, submit `https://jgmoli.com.au/sitemap.xml`.
5. Inspect `https://jgmoli.com.au/` with URL Inspection, run Test live URL, review the fetched/rendered page and request indexing if eligible.
6. Monitor the indexing report. A sitemap or indexing request does not guarantee indexing or a particular ranking/timeframe. Search Console is the authoritative place to diagnose crawling and indexing problems; absence from a search query alone does not establish the exact cause.

References: [Google sitemap guidance](https://developers.google.com/search/docs/crawling-indexing/sitemaps/build-sitemap), [request a recrawl](https://developers.google.com/search/docs/crawling-indexing/ask-google-to-recrawl), [noindex and crawl access](https://developers.google.com/search/docs/crawling-indexing/block-indexing).
