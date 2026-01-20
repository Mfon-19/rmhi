import asyncio

from .transform import run_transformations


def main() -> None:
    asyncio.run(run_transformations())


if __name__ == "__main__":
    main()
