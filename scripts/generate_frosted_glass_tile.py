#!/usr/bin/env python3
"""
Frosted Glass Tile Base Generator for StepLauncher
Generates high-resolution translucent frosted glass PNG texture with rounded corners,
light top/left highlights, dark bottom/right bevel shadows, and mid-gray 0.125 alpha fill.
"""

import os
import math
from PIL import Image, ImageDraw, ImageFilter

def generate_frosted_glass_tile(output_path, size=256, radius=48):
    # Create RGBA canvas
    img = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)

    # 1. Base Mid-Gray 0.125 Alpha Fill (32 / 255)
    bg_color = (160, 160, 160, 32)
    draw.rounded_rectangle([0, 0, size - 1, size - 1], radius=radius, fill=bg_color)

    # 2. Bevel & Gradient Highlights/Shadows
    for y in range(size):
        for x in range(size):
            # Calculate distance from rounded box edge
            dx = min(x, size - 1 - x)
            dy = min(y, size - 1 - y)
            
            # Check if pixel is within rounded rectangle mask
            inside = False
            if x < radius and y < radius:
                inside = (x - radius)**2 + (y - radius)**2 <= radius**2
            elif x >= size - radius and y < radius:
                inside = (x - (size - radius - 1))**2 + (y - radius)**2 <= radius**2
            elif x < radius and y >= size - radius:
                inside = (x - radius)**2 + (y - (size - radius - 1))**2 <= radius**2
            elif x >= size - radius and y >= size - radius:
                inside = (x - (size - radius - 1))**2 + (y - (size - radius - 1))**2 <= radius**2
            else:
                inside = True

            if not inside:
                img.putpixel((x, y), (0, 0, 0, 0))
                continue

            # Bevel factor based on proximity to top-left vs bottom-right
            # Top-Left direction vector
            top_left_dist = math.sqrt(x*x + y*y)
            max_dist = math.sqrt(size*size + size*size)
            norm_pos = top_left_dist / max_dist  # 0.0 at top-left, 1.0 at bottom-right

            # Edge proximity (0 at border, 1 deep inside)
            border_dist = min(dx, dy)
            if border_dist < 12:
                edge_factor = border_dist / 12.0
                
                # Top/Left highlight: Light gray (240, 240, 240) fading alpha from 0.5 (128) -> 0.125 (32)
                # Bottom/Right shadow: Dark gray (40, 40, 40) fading alpha from 0.5 (128) -> 0.125 (32)
                if norm_pos < 0.5:
                    # Light top-left edge
                    blend = (1.0 - norm_pos * 2.0)
                    r = int(160 + (240 - 160) * blend)
                    g = int(160 + (240 - 160) * blend)
                    b = int(160 + (240 - 160) * blend)
                    alpha = int((128 * (1.0 - edge_factor) + 32 * edge_factor))
                else:
                    # Dark bottom-right edge
                    blend = (norm_pos - 0.5) * 2.0
                    r = int(160 - (160 - 40) * blend)
                    g = int(160 - (160 - 40) * blend)
                    b = int(160 - (160 - 40) * blend)
                    alpha = int((128 * (1.0 - edge_factor) + 32 * edge_factor))

                img.putpixel((x, y), (r, g, b, alpha))

    # Apply subtle blur filter for authentic frosted glass dispersion
    img = img.filter(ImageFilter.GaussianBlur(radius=0.8))

    # Ensure output directory exists
    os.makedirs(os.path.dirname(output_path), exist_ok=True)
    img.save(output_path, 'PNG')
    print(f"✓ Generated frosted glass tile image at: {output_path}")

if __name__ == '__main__':
    target_path = '/data/data/com.termux/files/home/Projects/steplauncher/app-launcher/src/main/res/drawable/bg_frosted_glass_tile_base.png'
    generate_frosted_glass_tile(target_path)
