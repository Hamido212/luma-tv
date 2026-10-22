import sys
from pathlib import Path
from playwright.sync_api import sync_playwright

files = [Path(p).resolve() for p in sys.argv[1:]]
with sync_playwright() as p:
    browser = p.chromium.launch()
    page = browser.new_page(viewport={"width": 1920, "height": 1080})
    for svg in files:
        page.goto(svg.as_uri())
        page.screenshot(path=str(svg.with_suffix(".png")))
        print("rendered", svg.with_suffix(".png").name)
    browser.close()
