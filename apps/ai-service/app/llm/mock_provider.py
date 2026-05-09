class MockProvider:
    def generate(self, prompt: str) -> str:
        return f"Mock response for: {prompt[:120]}"
