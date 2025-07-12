This repository serves as an **archive and analysis vault** for all historical versions of core Java classes used in my chess engine projects.

Purpose

Over multiple projects and iterations of my Java chess engine (e.g., SeedV1, SeedV2, SeedV3, and others), I have developed many versions of key classes such as:

- `Board`: board state representation and manipulation
- `Gen`: move generation
- `Eval`: evaluation functions
- Supporting utility classes (`History`, `Sort`, `TTable`, etc.)

Each version often includes unique optimizations, design changes, or performance improvements. However, these improvements were not always merged across projects, leading to multiple variations with different strengths.

The purpose of this repository is to:

- **Centralize** all historical versions of these classes in one place.
- **Document** which versions exist and their origins.
- **Compare** the different implementations.
- **Identify** and eventually synthesize the "best-of" solutions for each class.
- Provide a structured dataset for analysis with AI tools (such as Codex) to extract the most efficient and effective code.

Repository Structure

- **/src**: Source code organized by package.
    - `board/`
    - `gen/`
    - `eval/`
    - `util/history/`
    - `util/sort/`
    - `util/ttable/`
- Each package contains multiple versions of the class, named to indicate the originating project (e.g., `Board_SeedV3.java`).
- Each package includes a `*_VERSIONS.md` file listing the versions it contains.

Important Notes

- **This repository is not intended to compile or run as-is.**
    - Many utility classes and imports are deliberately omitted, since the focus is on code comparison and analysis, not execution.
- **No code modifications** are performed in this repository beyond renaming files and organizing them into packages.
- **Performance-focused code** is preserved in its original form, even if it is not fully maintainable or conventional by typical Java standards.

Future Plans

This archive will be used as the basis for:

- Codex-powered analysis and merging of the best features from each version.
- Benchmarking and profiling of different approaches.
- Creation of a single consolidated, optimized version of each core class.

License

This project is licensed under the MIT License.

**Attribution Notice:** Portions of this code are original work by cohinteractive.  
If you reuse significant portions of this codebase, please provide attribution in your documentation, credits, or license files as:

“Portions © 2025 github.com/cohinteractive”
