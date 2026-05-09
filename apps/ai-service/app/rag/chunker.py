def chunk_text(text: str, chunk_size: int = 1200) -> list[str]:
    if chunk_size <= 0:
        raise ValueError("chunk_size must be positive")
    return [text[index:index + chunk_size] for index in range(0, len(text), chunk_size)]
