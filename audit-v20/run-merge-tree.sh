#!/usr/bin/env bash
set -euo pipefail
REPO="${1:?Provide the repository checkout}"
OUT="${2:?Provide the evidence output file}"
SHORT=086fbf91294f74fe4bd967256498ef94e92c501a
LONG=45eccdc73ce36d4298326498a3603e2363251fd6
BASE=ad8d03574c7f048b236c9d24f0c03ac54dffcbd7
mkdir -p "$(dirname "$OUT")"
{
  echo "v2.0 car-dealership virtual merge rehearsal"
  echo "short=$SHORT"
  echo "long=$LONG"
  echo "expected_merge_base=$BASE"
  git -C "$REPO" fetch --no-tags origin refs/heads/fix/atomic-account-create-20261029
  test "$(git -C "$REPO" rev-parse FETCH_HEAD)" = "$SHORT" || { echo "Short-branch SHA drift"; exit 8; }
  git -C "$REPO" fetch --no-tags origin refs/heads/security/postgres-commit-ack-transport-loss-20261029
  test "$(git -C "$REPO" rev-parse FETCH_HEAD)" = "$LONG" || { echo "Long-branch SHA drift"; exit 8; }
  for ref in "$SHORT" "$LONG" "$BASE"; do git -C "$REPO" cat-file -e "$ref^{commit}" || { echo "Missing commit $ref"; exit 8; }; done
  actual="$(git -C "$REPO" merge-base "$SHORT" "$LONG")"
  echo "actual_merge_base=$actual"
  test "$actual" = "$BASE" || { echo "Reviewed merge base drift"; exit 8; }
  set +e
  git -C "$REPO" merge-tree --write-tree "$SHORT" "$LONG"
  status=$?
  set -e
  echo "merge_tree_exit=$status"
  if ((status == 0)); then
    echo "Virtual merge clean; compiled integration and acceptance scenarios NOT YET RUN."
  else
    echo "Virtual merge failed or conflicted; no integration approved."
  fi
  exit "$status"
} > "$OUT" 2>&1