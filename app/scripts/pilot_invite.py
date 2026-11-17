"""Generate an email-bound invitation using the pilot signing secret."""
import argparse
import hmac
import os
import getpass


def invitation_code(email, secret):
    return hmac.digest(secret.encode("utf-8"), email.strip().lower().encode("utf-8"), "sha256").hex()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("email", help="The tester's allowlisted email")
    args = parser.parse_args()
    secret = os.environ.get("PILOT_INVITE_SECRET") or getpass.getpass("Pilot signing secret: ")
    if not secret.strip():
        parser.error("Provide a pilot signing secret")
    print(invitation_code(args.email, secret))


if __name__ == "__main__":
    main()
