from array import array
import importlib.util
from pathlib import Path
import unittest
import wave
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]


class AssetsTest(unittest.TestCase):
    def test_all_story_sounds_exist_are_quiet_and_faded(self):
        names = set()
        for story in (ROOT / 'app/src/main/assets/stories').glob('*.story'):
            for scene in story.read_text().strip().split('\n---\n')[1:]:
                names.add(scene.split('|', 1)[0])
        self.assertEqual(12, len(names))
        for name in names:
            with self.subTest(sound=name), wave.open(str(ROOT / 'app/src/main/assets/sounds' / (name + '.wav'))) as sound:
                self.assertEqual(1, sound.getnchannels())
                self.assertEqual(22050, sound.getframerate())
                self.assertEqual(2, sound.getsampwidth())
                data = array('h', sound.readframes(sound.getnframes()))
                self.assertGreater(len(data), 22050)
                self.assertLessEqual(max(abs(s) for s in data), 21300)
                self.assertGreater(max(abs(s) for s in data), 500)
                self.assertLess(max(abs(s) for s in data[-300:]), 1200)

    def test_sounds_are_reproducible(self):
        spec = importlib.util.spec_from_file_location('sounds', ROOT / 'scripts/generate_sounds.py')
        module = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(module)
        self.assertEqual(module.render('rain', .1), module.render('rain', .1))

    def test_only_github_variant_requests_install_permission(self):
        android = '{http://schemas.android.com/apk/res/android}'
        base = ET.parse(ROOT / 'app/src/main/AndroidManifest.xml').getroot()
        permissions = {e.get(android + 'name') for e in base.findall('uses-permission')}
        self.assertEqual({'android.permission.INTERNET', 'android.permission.RECORD_AUDIO'}, permissions)
        github = ET.parse(ROOT / 'app/src/github/AndroidManifest.xml').getroot()
        self.assertEqual('android.permission.REQUEST_INSTALL_PACKAGES', github.find('uses-permission').get(android + 'name'))
        self.assertEqual('false', base.find('application').get(android + 'allowBackup'))


if __name__ == '__main__':
    unittest.main()
