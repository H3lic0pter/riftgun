from pathlib import Path

from PIL import Image, ImageDraw


CANVAS_SIZE = 64
CENTER = CANVAS_SIZE // 2
LINE_COLOR = (0x9C, 0xD4, 0xE5, 0xFF)
OUTPUT_DIR = Path("reference/precision_radial_center_icons")


def translated(points: list[tuple[int, int]]) -> list[tuple[int, int]]:
    return [(CENTER + x, CENTER + y) for x, y in points]


def outline(kind: str, horizontal_offset: int = 0) -> list[tuple[int, int]]:
    if kind == "vertical":
        return [(-10, -16), (10, -12), (10, 14), (-10, 10)]
    return [
        (-16, horizontal_offset - 1),
        (2, horizontal_offset - 9),
        (16, horizontal_offset + 8),
        (-2, horizontal_offset + 16),
    ]


def draw_pattern(kind: str, horizontal_offset: int = 0, arrow: str | None = None) -> Image.Image:
    image = Image.new("RGBA", (CANVAS_SIZE, CANVAS_SIZE), (0, 0, 0, 0))
    draw = ImageDraw.Draw(image)
    points = translated(outline(kind, horizontal_offset))
    draw.line(points + [points[0]], fill=LINE_COLOR, width=1)

    if arrow is not None:
        from_y, to_y = (-13, 13) if arrow == "down" else (13, -13)
        head_y = to_y + (-4 if arrow == "down" else 4)
        draw.line(translated([(0, from_y), (0, to_y)]), fill=LINE_COLOR, width=1)
        draw.line(translated([(0, to_y), (-4, head_y)]), fill=LINE_COLOR, width=1)
        draw.line(translated([(0, to_y), (4, head_y)]), fill=LINE_COLOR, width=1)
    return image


def main() -> None:
    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)
    patterns = {
        "front_in_front.png": draw_pattern("vertical"),
        "front_below_feet.png": draw_pattern("horizontal", horizontal_offset=7),
        "front_above_head.png": draw_pattern("horizontal", horizontal_offset=-7),
        "remote_sideways.png": draw_pattern("vertical"),
        "remote_top_down.png": draw_pattern("horizontal", arrow="down"),
        "remote_bottom_up.png": draw_pattern("horizontal", arrow="up"),
    }
    for name, image in patterns.items():
        image.save(OUTPUT_DIR / name)


if __name__ == "__main__":
    main()
