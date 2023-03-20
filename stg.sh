#!/bin/bash

# Exit on any failure
set -e

flyctl deploy --local-only --config fly-stg.toml
