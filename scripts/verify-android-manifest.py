#!/usr/bin/env python3
"""Fail CI if app manifest/backup configuration widens the privacy boundary."""
import pathlib
import sys
import xml.etree.ElementTree as ET

ANDROID = "{http://schemas.android.com/apk/res/android}"
ROOT = pathlib.Path(__file__).resolve().parents[1]

def require(condition, message):
    if not condition:
        raise SystemExit("SECURITY CHECK FAILED: " + message)

manifest = ET.parse(ROOT / "app/src/main/AndroidManifest.xml").getroot()
require(manifest.tag == "manifest", "unexpected manifest root")
require(not manifest.findall("uses-permission"), "permissions require review")
require(not manifest.findall("uses-permission-sdk-23"), "SDK-specific permissions require review")
app = manifest.find("application")
require(app is not None, "missing application")
for attribute, expected in (
    ("allowBackup", "false"),
    ("fullBackupContent", "false"),
    ("dataExtractionRules", "@xml/data_extraction_rules"),
    ("usesCleartextTraffic", "false"),
):
    require(app.get(ANDROID + attribute) == expected, attribute + " must be " + expected)

components = ("activity", "activity-alias", "service", "receiver", "provider")
for component in components:
    for node in app.findall(component):
        name = node.get(ANDROID + "name", "")
        exported = node.get(ANDROID + "exported")
        if name == ".MainActivity" and component == "activity":
            require(exported == "true", "launcher activity must be exported")
            filters = node.findall("intent-filter")
            actions = {a.get(ANDROID + "name") for f in filters for a in f.findall("action")}
            categories = {c.get(ANDROID + "name") for f in filters for c in f.findall("category")}
            require("android.intent.action.MAIN" in actions, "launcher missing MAIN")
            require("android.intent.category.LAUNCHER" in categories, "launcher missing LAUNCHER")
        else:
            require(exported != "true", "unexpected exported component: " + name)
require(len(app.findall("activity")) == 1, "additional activities require security review")
rules = ET.parse(ROOT / "app/src/main/res/xml/data_extraction_rules.xml").getroot()
require(rules.tag == "data-extraction-rules", "invalid extraction rules")
for part in ("cloud-backup", "device-transfer"):
    node = rules.find(part)
    require(node is not None, "missing " + part + " exclusion")
    require(
        any(e.get("domain") == "root" and e.get("path") == "."
            for e in node.findall("exclude")),
        "missing root exclusion from " + part,
    )
print("Manifest security: no permissions, no cleartext, safe exports, backup excluded")
