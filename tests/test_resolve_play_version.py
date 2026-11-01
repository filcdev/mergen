import importlib.util
from pathlib import Path
import unittest

script = Path(__file__).resolve().parents[1] / '.github/scripts/resolve_play_version.py'
spec = importlib.util.spec_from_file_location('resolve_play_version', script)
module = importlib.util.module_from_spec(spec)
spec.loader.exec_module(module)


class VersionResolutionTests(unittest.TestCase):
    def test_duplicate_1015_is_not_reused(self):
        codes = module.get_track_version_codes([
            {'track': 'alpha', 'releases': [{'versionCodes': ['1015']}]},
            {'track': 'production', 'releases': [{'versionCodes': ['1014']}]},
        ])
        self.assertEqual(module.next_version(codes, ['v0.1.15']), (1016, '0.1.16'))

    def test_newest_store_code_wins(self):
        self.assertEqual(module.next_version([1020], ['v0.1.15']), (1021, '0.1.21'))

    def test_newest_tag_wins(self):
        self.assertEqual(module.next_version([1015], ['v0.1.30']), (1031, '0.1.31'))

    def test_ignores_nonsemantic_tags(self):
        self.assertEqual(module.next_version([1015], ['latest', 'nightly']), (1016, '0.1.16'))

    def test_patch_rollover(self):
        self.assertEqual(module.next_version([999], []), (1000, '0.1.0'))

    def test_play_limit(self):
        with self.assertRaises(ValueError):
            module.next_version([2_100_000_000], [])


if __name__ == '__main__':
    unittest.main()
