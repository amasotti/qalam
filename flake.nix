{
  description = "qalam — personal Arabic learning tool dev shell";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";
    flake-utils.url = "github:numtide/flake-utils";
  };

  outputs = { self, nixpkgs, flake-utils }:
    flake-utils.lib.eachDefaultSystem (system:
      let
        pkgs = nixpkgs.legacyPackages.${system};
      in
      {
        devShells.default = pkgs.mkShell {
          packages = with pkgs; [
            # JVM — Gradle wrapper downloads its own Gradle, but the JDK must be here
            jdk25

            # Task runner — all dev commands live in justfile
            just

            # Secrets — must be configured before any `just` recipe runs
            doppler

            # Frontend runtime + package manager
            nodejs_24
            pnpm

            # Database client — useful for manual inspection and migration debugging
            postgresql_17

            # Changelog generation — mirrors what release CI does
            git-cliff
          ];

          # Gradle and the JVM must agree on JAVA_HOME
          JAVA_HOME = "${pkgs.jdk25}";

          shellHook = ''
            echo "qalam dev shell"
            echo "  java   $(java -version 2>&1 | head -1 | cut -d'"' -f2)"
            echo "  node   $(node --version)"
            echo "  pnpm   $(pnpm --version)"
            echo "  just   $(just --version)"
            echo "  psql   $(psql --version | cut -d' ' -f3)"
            echo ""
            echo "  just start-db   postgres only"
            echo "  just backend    Ktor with hot reload"
            echo "  just frontend   SvelteKit dev server"
            echo "  just run        full stack"
          '';
        };
      }
    );
}
