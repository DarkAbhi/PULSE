import telebot
from telebot import types
import requests
from datetime import datetime, timedelta, timezone
import logging
import os
from functools import wraps
import constants
import api_constants

logger = telebot.logger
telebot.logger.setLevel(logging.DEBUG)
allowed_user_id = int(os.environ["TELEGRAM_ALLOWED_USER_ID"])
bot = telebot.TeleBot(os.environ.get("BOT_API_KEY"), parse_mode=None)
india_time = timezone(timedelta(hours=5, minutes=30))


def owner_only(handler):
    @wraps(handler)
    def guarded(message):
        if getattr(getattr(message, "from_user", None), "id", None) != allowed_user_id:
            return
        return handler(message)
    return guarded


def record_entry(message, endpoint, success, payload=None):
    try:
        response = requests.post(
            f'{api_constants.BASE_URL}{endpoint}', json=payload, timeout=10)
        if response.status_code == 201:
            reply = f'{success} {datetime.now(india_time).date()}.'
        elif response.status_code == 400:
            reply = response.json().get("error", "Invalid entry.")
        else:
            logger.error("Backend returned status %s for %s", response.status_code, endpoint)
            reply = 'Unable to save your entry. Please try again.'
    except (requests.exceptions.RequestException, ValueError):
        logger.exception("Failed to save entry at %s", endpoint)
        reply = 'Unable to save your entry. Please try again.'
    bot.send_message(message.chat.id, reply, reply_markup=types.ReplyKeyboardRemove())


@bot.message_handler(commands=['start'])
@owner_only
def response_to_start_action(message):
    markup = types.InlineKeyboardMarkup()
    markup.add(types.InlineKeyboardButton(
        "Checkout author", url=constants.AUTHOR_WEBSITE))
    bot.send_message(message.chat.id, "Howdy, you can choose any of these and update your life data tracker.\n\n" +
                     "/quick - Show quick entry options.\n" +
                     "/sport - Mark a sport you played.", reply_markup=markup)


@bot.message_handler(commands=['sport'])
@owner_only
def response_to_sport_action(message):
    markup = types.ReplyKeyboardMarkup(row_width=2, selective=False)
    itembtn1 = types.KeyboardButton(f'{constants.CRICKET}')
    itembtn2 = types.KeyboardButton(f'{constants.FOOTBALL}')
    itembtn3 = types.KeyboardButton(f'{constants.BADMINTON}')
    itembtn4 = types.KeyboardButton(f'{constants.BACK}')
    markup.add(itembtn1, itembtn2, itembtn3, itembtn4)
    bot.send_message(message.chat.id, "What did you play?",
                     reply_markup=markup)
    bot.register_next_step_handler(message, handle_sport_played)


@bot.message_handler(commands=['quick'])
@owner_only
def response_to_quick_action(message):
    markup = types.ReplyKeyboardMarkup(row_width=2, selective=False)
    itembtn1 = types.KeyboardButton(f'{constants.MARK_WORKOUT}')
    itembtn2 = types.KeyboardButton(f'{constants.MARK_MEDITATION}')
    itembtn3 = types.KeyboardButton(f'{constants.CANCEL}')
    markup.add(itembtn1, itembtn2, itembtn3)
    bot.send_message(message.chat.id, "What would you like to mark?",
                     reply_markup=markup)
    bot.register_next_step_handler(message, handle_quick_options)




@owner_only
def handle_sport_played(message):
    sports = {
        constants.CRICKET: "cricket",
        constants.FOOTBALL: "football",
        constants.BADMINTON: "badminton",
    }
    if message.text in sports:
        record_entry(message, api_constants.ADD_SPORT_ENDPOINT,
                     f'{message.text} played on', {"sport": sports[message.text]})
    elif message.text == constants.BACK:
        bot.send_message(
            message.chat.id, "Okay.", reply_markup=types.ReplyKeyboardRemove())
    else:
        bot.send_message(message.chat.id, 'Invalid input.', reply_markup=types.ReplyKeyboardRemove())

@owner_only
def handle_quick_options(message):
    if (message.text == f'{constants.MARK_WORKOUT}'):
        record_entry(message, api_constants.ADD_WORKOUT_ENDPOINT, 'Gym entry made for')
    elif (message.text == f'{constants.MARK_MEDITATION}'):
        record_entry(message, api_constants.ADD_MEDITATION_ENDPOINT, 'Meditation entry made for')
    elif message.text == f'{constants.CANCEL}' or message.text == f'{constants.BACK}':
        bot.send_message(message.chat.id, "Okay.",
                         reply_markup=types.ReplyKeyboardRemove())
    else:
        logger.error(f"Quick options invalid input - {message.text}")
        bot.send_message(
            message.chat.id, 'Invalid input.', reply_markup=types.ReplyKeyboardRemove())


# Enable saving next step handlers to file "./.handlers-saves/step.save".
# Delay=2 means that after any change in next step handlers (e.g. calling register_next_step_handler())
# saving will hapen after delay 2 seconds.
if __name__ == '__main__':
    bot.enable_save_next_step_handlers(delay=5)
    bot.load_next_step_handlers()
    bot.infinity_polling()
