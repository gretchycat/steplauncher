#!/usr/bin/env python3
"""
Paperclip Anchor Icon Generator for StepLauncher Workspace Dock Anchor
Generates a 256x256 transparent PNG paperclip tilted at a 45° angle with metallic grayscale shading.
"""

import os
from PIL import Image, ImageDraw, ImageFilter

def generate_paperclip_icon(output_path, size=256):
    # Create unrotated canvas (drawing paperclip vertically first)
    canvas = Image.new('RGBA', (size * 2, size * 2), (0, 0, 0, 0))
    draw = ImageDraw.Draw(canvas)

    cx = size
    cy = size
    stroke = 14

    # Outer loop bounds
    outer_w = 48
    outer_h = 130
    
    # Inner loop bounds
    inner_w = 26
    inner_h = 90

    # Draw metallic outer rounded loop
    # Top arc
    draw.arc([cx - outer_w, cy - outer_h, cx + outer_w, cy - outer_h + outer_w * 2], 180, 0, fill=(210, 210, 210, 255), width=stroke)
    # Right vertical line
    draw.line([(cx + outer_w, cy - outer_h + outer_w), (cx + outer_w, cy + outer_h - outer_w)], fill=(180, 180, 180, 255), width=stroke)
    # Bottom arc
    draw.arc([cx - outer_w, cy + outer_h - outer_w * 2, cx + outer_w, cy + outer_h], 0, 180, fill=(140, 140, 140, 255), width=stroke)
    # Left vertical line
    draw.line([(cx - outer_w, cy + outer_h - outer_w), (cx - outer_w, cy - inner_h + inner_w)], fill=(160, 160, 160, 255), width=stroke)

    # Inner loop
    draw.arc([cx - inner_w, cy - inner_h, cx + inner_w, cy - inner_h + inner_w * 2], 180, 0, fill=(230, 230, 230, 255), width=stroke)
    draw.line([(cx + inner_w, cy - inner_h + inner_w), (cx + inner_w, cy + inner_h - inner_w)], fill=(170, 170, 170, 255), width=stroke)
    draw.arc([cx - inner_w, cy + inner_h - inner_w * 2, cx + inner_w, cy + inner_h], 0, 180, fill=(110, 110, 110, 255), width=stroke)

    # Highlight overlay for 3D metallic sheen
    draw.arc([cx - outer_w + 2, cy - outer_h + 2, cx + outer_w - 2, cy - outer_h + outer_w * 2 - 2], 180, 0, fill=(255, 255, 255, 180), width=4)
    draw.line([(cx + outer_w - 3, cy - outer_h + outer_w), (cx + outer_w - 3, cy + outer_h - outer_w)], fill=(255, 255, 255, 160), width=3)

    # Rotate by 45 degrees
    rotated = canvas.rotate(45, resample=Image.BICUBIC, expand=False)

    # Crop center back to original size
    crop_x = (size * 2 - size) // 2
    crop_y = (size * 2 - size) // 2
    final_img = rotated.crop((crop_x, crop_y, crop_x + size, crop_y + size))

    # Save to output path
    os.makedirs(os.path.dirname(output_path), exist_ok=True)
    final_img.save(output_path, 'PNG')
    print(f"✓ Generated 45° tilted paperclip anchor icon at: {output_path}")

if __name__ == '__main__':
    target_path = '/data/data/com.termux/files/home/Projects/steplauncher/app-launcher/src/main/res/drawable/ic_dock_anchor_paperclip.png'
    generate_paperclip_icon(target_path)
