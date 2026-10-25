# 🤖 AI Job Hunter Agent (Java Backend)

Автономный фоновый агент на Java & Spring Boot, предназначенный для мониторинга IT-вакансий, фильтрации нецелевых позиций, семантического скоринга стека с помощью Google Gemini AI и отправки персональных алертов в Telegram.

---

## 🚀 Возможности

- **Мультисорсинг вакансий (Strategy Pattern):**
  - RSS-лента Хабр Карьеры (`HabrCareerService`).
  - Парсинг открытых каналов Telegram через веб-интерфейс (`TelegramChannelSource`).
- **Многоуровневая фильтрация:**
  - Префильтрация по стоп-словам (QA, Frontend, Mobile, DevOps, C#, PHP и др.).
  - Семантическая оценка соответствия резюме требованиям вакансии через **Gemini AI**.
- **Дедупликация:**
  - Локальное хранение обработанных идентификаторов (`processed_ids.txt`) для предотвращения повторного скоринга и спама.
- **Интерактивные уведомления:**
  - Отправка карточек вакансий в Telegram с inline-кнопкой перехода.
  - Генерация краткого сопроводительного письма (Cover Letter) под конкретную вакансию.
  - Сохранение истории откликов в формате Markdown (`applications.md`).

---

## 🛠 Стек технологий

- **Язык:** Java 21
- **Фреймворк:** Spring Boot 3.4.2
- **AI / LLM:** Google Gemini API (`gemini-3.5-flash-lite`)
- **Скрапинг & Парсинг:** Jsoup, Jackson
- **HTTP Клиент:** Spring RestTemplate
- **Сборка:** Maven

---

## ⚙️ Настройка и запуск

### 1. Переменные окружения

Создайте локальный файл `.env` или передайте переменные в Run Configuration IDE:

```env
TELEGRAM_BOT_TOKEN=your_bot_token_here
TELEGRAM_CHAT_ID=your_chat_id_here
GEMINI_API_KEY=your_gemini_api_key_here```

2. Сборка проекта
