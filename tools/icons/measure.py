"""Measure visible ink of each converted mark (240 square): bbox and fill fraction inside it."""
import json
import sys
from io import BytesIO

from PIL import Image
from playwright.sync_api import sync_playwright

squares = json.load(open(sys.argv[1]))
out = {}
with sync_playwright() as p:
    browser = p.chromium.launch()
    page = browser.new_page(viewport={"width": 240, "height": 240})
    for name, items in squares.items():
        body = []
        for it in items:
            a = f'd="{it["d"]}" '
            a += 'fill="#fff" ' if it["fill"] else 'fill="none" '
            if it["rule"] == "evenodd":
                a += 'fill-rule="evenodd" '
            if it["stroke"]:
                a += f'stroke="#fff" stroke-width="{it["sw"]}" stroke-linecap="{it["cap"]}" stroke-linejoin="{it["join"]}" '
            body.append(f"<path {a}/>")
        html = ('<html><body style="margin:0;background:#000">'
                '<svg width="240" height="240" viewBox="0 0 240 240" xmlns="http://www.w3.org/2000/svg">'
                + "".join(body) + "</svg></body></html>")
        page.set_content(html)
        img = Image.open(BytesIO(page.screenshot())).convert("L")
        px = img.load()
        xs, ys, ink = [], [], 0.0
        for y in range(240):
            for x in range(240):
                v = px[x, y]
                if v > 24:
                    xs.append(x); ys.append(y)
                ink += v / 255.0
        if not xs:
            continue
        x0, x1, y0, y1 = min(xs), max(xs) + 1, min(ys), max(ys) + 1
        w, h = x1 - x0, y1 - y0
        out[name] = {"x": x0, "y": y0, "w": w, "h": h, "fill": round(ink / (w * h), 4)}
    browser.close()
json.dump(out, open(sys.argv[2], "w"), indent=1)
print(json.dumps(out))
