# DriveTrack

Android GPS journey recorder. Builds are signed using the encrypted GitHub Actions secret `DRIVETRACK_SIGNING_BUNDLE`.

**Keep the original permanent signing backup safe.** Never commit or share the keystore, its passwords, or the base64 signing bundle.

## Updating from early beta versions

Pre-v0.6 APKs use separate debug signatures. Back up journey records before uninstalling the old app. Permanently signed builds from v0.6 onward can update in place if you preserve and reuse the signing key.
