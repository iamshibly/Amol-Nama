"""Exercise five HTTP cookie jars and two app instances on a disposable pilot database."""
import concurrent.futures
import datetime
from html.parser import HTMLParser
import http.cookiejar
import json
import os
import secrets
import shlex
import subprocess
import threading
import urllib.error
import urllib.parse
import urllib.request

BASE = os.environ.get("PILOT_SMOKE_URL", "http://localhost:8080")
SECOND = os.environ.get("PILOT_SMOKE_SECOND_URL", "http://localhost:8081")
EMAILS = [f"pilot{i}@example.test" for i in range(1, 7)]
PASSWORD = secrets.token_urlsafe(24)
TODAY = datetime.date.today()
TAG = "Pilot smoke " + secrets.token_hex(4)


class CsrfParser(HTMLParser):
    token = None

    def handle_starttag(self, tag, attrs):
        attrs = dict(attrs)
        if tag == "input" and attrs.get("name") == "_csrf":
            self.token = attrs.get("value")


class Client:
    def __init__(self):
        self.opener = urllib.request.build_opener(
            urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar()))

    def get(self, path, base=BASE):
        with self.opener.open(base + path, timeout=30) as response:
            return urllib.parse.urlsplit(response.url).path, response.read().decode()

    def post(self, path, fields, form="/dashboard", base=BASE):
        parser = CsrfParser()
        parser.feed(self.get(form, base)[1])
        assert parser.token, f"No CSRF token in {form}"
        payload = urllib.parse.urlencode(fields | {"_csrf": parser.token}).encode()
        with self.opener.open(base + path, data=payload, timeout=30) as response:
            return urllib.parse.urlsplit(response.url).path, response.read().decode()

    def register(self, email, name):
        result = self.post("/register", {
            "email": email, "name": name, "password": PASSWORD,
            "confirmPassword": PASSWORD, "currentSemester": "Fall smoke",
        }, "/register")
        assert result[0] == "/login", f"Registration failed for {name}"

    def login(self, email, name):
        path, body = self.post("/login", {"username": email, "password": PASSWORD}, "/login")
        assert path == "/dashboard" and name in body, f"Login failed for {name}"

    def export(self):
        return json.loads(self.get("/profile/export")[1])


def exercise_student(index, barrier):
    name = f"Pilot Student {index}"
    client = Client()
    barrier.wait(timeout=30)
    client.register(EMAILS[index - 1], name)
    client.login(EMAILS[index - 1], name)
    for entry_index in range(3):
        fields = {
            "title": f"{TAG} / student {index} / entry {entry_index}",
            "description": "Submitted an assignment late.",
            "category": "ASSIGNMENT", "level": "HIGH", "status": "NEW",
            "regretDate": TODAY.isoformat(),
            "semester": "Fall smoke" if entry_index < 2 else "Spring smoke",
            "sharedAnonymously": "true" if index == 1 and entry_index == 0 else "false",
        }
        assert client.post("/afsos/save", fields, "/afsos/new")[0] == "/afsos"
        entry = next(item for item in client.export()["reflections"] if item["title"] == fields["title"])
        fields.update(id=entry["id"], lessonLearned="Use the course calendar.", status="LESSON_LEARNED")
        assert client.post("/afsos/save", fields, f"/afsos/edit/{entry['id']}")[0] == "/afsos"

    data = client.export()
    assert len(data["reflections"]) == 3
    source = data["reflections"][0]["id"]
    new_promise = f"/promises/new?sourceAfsosId={source}"
    path, body = client.get(new_promise)
    assert path == "/promises/new" and "Follow up on:" in body
    assert client.post("/promises/save", {
        "title": "Use the course calendar", "sourceAfsosId": source,
        "targetDate": (TODAY + datetime.timedelta(days=1)).isoformat(),
    }, new_promise)[0] == "/promises"
    promise = client.export()["promises"][0]
    assert promise["source_afsos_id"] == source and promise["semester"] == "Fall smoke"
    assert client.post(f"/promises/complete/{promise['id']}", {}, "/promises")[0] == "/promises"
    assert client.export()["promises"][0]["status"] == "COMPLETED"
    path, report = client.get("/afsos/report?semester=Spring+smoke")
    assert path == "/afsos/report" and f"student {index} / entry 2" in report
    assert f"student {index} / entry 0" not in report
    assert name in client.get("/dashboard", SECOND)[1], "Shared JDBC session did not reach the second instance"
    return client, source, promise["id"]


def execute_sql(statement):
    override = os.environ.get("PILOT_SMOKE_SQL_COMMAND")
    command = shlex.split(override) if override else [
        "docker", "exec", os.environ["PILOT_SMOKE_MYSQL_CONTAINER"],
        "mysql", "-N", "-uroot", "-pci-only", "afsos_nama_ci", "-e",
    ]
    return subprocess.check_output([*command, statement], text=True)


def main():
    with concurrent.futures.ThreadPoolExecutor(max_workers=5) as pool:
        barrier = threading.Barrier(5)
        students = list(pool.map(lambda index: exercise_student(index, barrier), range(1, 6)))
        first, post_id, promise_id = students[0]
        second = students[1][0]
        assert TAG not in second.get("/confessions")[1]
        assert second.get(f"/afsos/details/{post_id}")[0] == "/afsos"
        assert second.get(f"/promises/edit/{promise_id}")[0] == "/promises"
        assert second.post(f"/afsos/delete/{post_id}", {}, "/afsos")[0] == "/afsos"
        assert second.post(f"/promises/delete/{promise_id}", {}, "/promises")[0] == "/promises"
        assert second.post("/afsos/save", {
            "id": post_id, "title": "Unauthorized change", "description": "Some text",
            "category": "ASSIGNMENT", "level": "HIGH", "status": "NEW", "regretDate": TODAY.isoformat(),
        }, "/afsos/new")[0] == "/afsos"
        owner = first.export()
        assert len(owner["reflections"]) == 3 and len(owner["promises"]) == 1
        assert all(entry["title"].startswith(TAG) for entry in owner["reflections"])
        try:
            second.get("/admin")
        except urllib.error.HTTPError as error:
            assert error.code == 403
        else:
            raise AssertionError("Student accessed /admin")

        moderator = Client()
        moderator.register(EMAILS[-1], "Pilot Moderator")
        execute_sql("UPDATE users SET role='ADMIN' WHERE email='pilot6@example.test'")
        moderator.login(EMAILS[-1], "Pilot Moderator")
        assert moderator.post(f"/admin/confessions/{post_id}/approve", {}, "/admin")[0] == "/admin"

        def vote(item):
            client = item[0]
            for base in (BASE, SECOND):
                assert client.post(f"/confessions/{post_id}/react/SAME_BHAI", {}, "/confessions", base)[0] == "/confessions"

        list(pool.map(vote, students))
        assert "Anonymous Student" in first.get("/confessions")[1]
        assert len(first.export()["reactions"]) == 1
        result = execute_sql(f"SELECT COUNT(*) FROM afsos_reactions WHERE afsos_id={int(post_id)}")
        rows = [line.strip() for line in result.splitlines() if line.strip().isdecimal()]
        assert rows == ["5"], f"Expected five distinct reactions, found {rows}"
        assert second.post(f"/confessions/{post_id}/reaction/remove", {}, "/confessions")[0] == "/confessions"
        assert not second.export()["reactions"]
        assert second.post("/logout", {})[0] == "/login"
        assert second.get("/dashboard", SECOND)[0] == "/login"
        last_student = students[-1][0]
        result = execute_sql(f"SELECT id FROM users WHERE email='{EMAILS[4]}'")
        target_id = next(int(line) for line in result.splitlines() if line.strip().isdecimal())
        assert moderator.post(f"/admin/users/{target_id}/toggle", {}, "/admin")[0] == "/admin"
        assert last_student.get("/dashboard", SECOND)[0] == "/login"
    print("Five accounts passed registration, login, CRUD, isolation, reporting, moderation, concurrent idempotent reactions, and cross-instance sessions.")


if __name__ == "__main__":
    main()
