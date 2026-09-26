import importlib
import logging
import os
import sys
import types
import unittest
from unittest.mock import patch


class FakeBot:
    def __init__(self, *_args, **_kwargs):
        self.messages = []

    def message_handler(self, **_kwargs):
        return lambda handler: handler

    def send_message(self, chat_id, text, **_kwargs):
        self.messages.append((chat_id, text))


class BotEndpointsTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        telebot = types.ModuleType("telebot")
        telebot.logger = logging.getLogger("test-bot")
        telebot.TeleBot = FakeBot
        telebot.types = types.SimpleNamespace(ReplyKeyboardRemove=lambda: None)
        requests = types.ModuleType("requests")
        requests.exceptions = types.SimpleNamespace(RequestException=Exception)
        with patch.dict(os.environ, {"TELEGRAM_ALLOWED_USER_ID": "42"}), patch.dict(
                sys.modules, {"telebot": telebot, "requests": requests}):
            cls.main = importlib.import_module("main")
        cls.requests = requests

    def setUp(self):
        self.main.bot.messages.clear()
        self.message = types.SimpleNamespace(
            chat=types.SimpleNamespace(id=42), from_user=types.SimpleNamespace(id=42))

    def test_other_users_are_ignored_before_any_action(self):
        self.message.from_user.id = 99
        self.message.text = "Mark workout"
        self.requests.post = lambda *_args, **_kwargs: self.fail("backend was called")
        for handler in (
                self.main.response_to_start_action,
                self.main.response_to_quick_action,
                self.main.response_to_sport_action,
                self.main.handle_quick_options,
                self.main.handle_sport_played):
            with self.subTest(handler=handler.__name__):
                handler(self.message)
        self.assertEqual(self.main.bot.messages, [])

    def test_bot_commands_use_backend_contract(self):
        cases = [
            ("Mark workout", self.main.handle_quick_options, "/api/workout/today", None, "Gym"),
            ("Mark meditation", self.main.handle_quick_options, "/api/meditation/today", None, "Meditation"),
            ("Cricket", self.main.handle_sport_played, "/api/sport/today", {"sport": "cricket"}, "Cricket"),
            ("Football", self.main.handle_sport_played, "/api/sport/today", {"sport": "football"}, "Football"),
            ("Badminton", self.main.handle_sport_played, "/api/sport/today", {"sport": "badminton"}, "Badminton"),
        ]
        for choice, handler, endpoint, payload, expected in cases:
            with self.subTest(choice=choice):
                self.message.text = choice
                calls = []

                def post(url, **kwargs):
                    calls.append((url, kwargs))
                    return types.SimpleNamespace(status_code=201)

                self.requests.post = post
                handler(self.message)
                self.assertEqual(calls, [(f'{self.main.api_constants.BASE_URL}{endpoint}',
                                           {"json": payload, "timeout": 10})])
                self.assertIn(expected, self.main.bot.messages[-1][1])

    def test_backend_error_always_gets_a_reply(self):
        self.requests.post = lambda *_args, **_kwargs: types.SimpleNamespace(status_code=500)
        self.main.record_entry(self.message, "/api/workout/today", "Gym entry made for")
        self.assertIn("Unable to save", self.main.bot.messages[-1][1])

        self.requests.post = lambda *_args, **_kwargs: types.SimpleNamespace(
            status_code=400, json=lambda: {"error": "already meditated today"})
        self.main.record_entry(self.message, "/api/meditation/today", "Meditation entry made for")
        self.assertEqual(self.main.bot.messages[-1][1], "already meditated today")


if __name__ == "__main__":
    unittest.main()
