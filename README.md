<div align="center">

# 📚 dziennikGG

**Alternatywna aplikacja mobilna do e-dziennika VULCAN / eduVULCAN / Librus Synergia**

> Szybki, nowoczesny i w pełni funkcjonalny klient dziennika elektronicznego dla polskich uczniów i rodziców.

[![Android](https://img.shields.io/badge/Platform-Android-3DDC84?style=flat&logo=android)](https://github.com/beniuk1290/edziennik)
[![iOS](https://img.shields.io/badge/Platform-iOS-000000?style=flat&logo=apple)](https://github.com/beniuk1290/edziennik)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-7F52FF?style=flat&logo=kotlin)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/UI-Compose%20Multiplatform-4285F4?style=flat)](https://www.jetbrains.com/compose-multiplatform/)
[![License](https://img.shields.io/github/license/beniuk1290/edziennik)](LICENSE)
[![Issues](https://img.shields.io/github/issues/beniuk1290/edziennik)](https://github.com/beniuk1290/edziennik/issues)
[![Stars](https://img.shields.io/github/stars/beniuk1290/edziennik)](https://github.com/beniuk1290/edziennik/stargazers)

</div>

---

## 🔍 O projekcie

**dziennikGG** to open-source'owa aplikacja mobilna stworzona w **Kotlin Multiplatform** z **Compose Multiplatform**, umożliwiająca dostęp do polskiego e-dziennika szkolnego. Aplikacja wspiera jednocześnie systemy **VULCAN Hebe**, **eduVULCAN (Prometheus)** oraz **Librus Synergia** — bez konieczności korzystania z oficjalnych, ograniczonych aplikacji.


---

## ✨ Funkcje

### 📊 Panel główny
- Powitanie z imieniem ucznia
- **Szczęśliwy numerek** — wyświetlany na pierwszym planie
- **Ostatnie oceny** — z ostatnich 7 dni z kolorowymi oznaczeniami
- **Nadchodzące sprawdziany** i **zadania domowe** na najbliższy tydzień

### 📝 Oceny
- Pełna lista ocen z bieżącego okresu, pogrupowana według przedmiotów
- Średnie ocen — zarówno serwera, jak i obliczane lokalnie
- Kolorowe karty: 🟢 wysokie, 🟡 średnie, 🔴 niskie oceny
- Waga, data i wartość każdej oceny

### 📅 Plan lekcji
- Tygodniowy plan z nawigacją (poprzedni/następny tydzień)
- **Zastępstwa** — wyróżnione żółtym tłem
- **Odwołane lekcje** — oznaczone kolorem błędu
- Numer lekcji, przedmiot, nauczyciel, sala, godziny

### 📋 Sprawdziany i kartkówki
- Tygodniowy widok z rozróżnieniem typów (sprawdzian / kartkówka)
- Przedmiot, opis, data, nauczyciel
- Kolorowe badge'y z typem sprawdzianu

### 📚 Zadania domowe
- Tygodniowy przegląd zadań domowych
- Informacja o wymaganej odpowiedzi

### ⚠️ Uwagi
- Lista uwag pozytywnych i negatywnych
- Kolorowe oznaczenia: pozytywne (zielone), negatywne (bursztynowe)
- Kategoria, data, treść, nauczyciel

### 📢 Ogłoszenia
- Tytuł, data, nadawca, treść ogłoszeń szkolnych

### 💬 Wiadomości
- Trzy zakładki: **Odebrane**, **Wysłane**, **Usunięte**
- Widok szczegółów wiadomości
- Status przeczytania i załączniki
- Obsługa przez Hebe API oraz API Prometheus (eduVULCAN)

### 🔄 Aktualizacje w aplikacji
- Automatyczne sprawdzanie nowych wersji na GitHubie
- Pobieranie i instalacja APK bezpośrednio z aplikacji

---

## 🛠️ Technologie

| Technologia | Wersja | Opis |
|---|---|---|
| **Kotlin** | 2.3.20 | Język programowania |
| **Compose Multiplatform** | 1.10.3 | UI cross-platform |
| **Material 3** | 1.10.0-alpha05 | System designu |
| **Ktor** | 3.4.1 | HTTP client |
| **Koin** | 4.1.1 | Dependency injection |
| **kotlinx.serialization** | 1.10.0 | Serializacja JSON |
| **kotlinx-datetime** | 0.7.1 | Obsługa dat i czasu |
| **DataStore** | 1.2.1 | Trwałe przechowywanie danych |

---

## 🏗️ Architektura

```
UI (Compose) ← StateFlow ← ViewModel ← ApiSession ← SzpontApi (Hebe/Librus/Prometheus)
                                                  ↕
                                            SessionStorage (DataStore)
```

- **MVVM** — Model-View-ViewModel z reaktywnym przepływem danych
- **Koin** — dependency injection dla wszystkich serwisów
- **Platform Abstraction** — `expect`/`actual` dla Android/iOS
- **Typed Navigation** — Navigation3 z bezpiecznymi typowo trasami

---

## 🚀 Instalacja

### Android
1. Przejdź do [Releases](https://github.com/beniuk1290/edziennik/releases)
2. Pobierz najnowszy plik `.apk`
3. Zainstaluj na urządzeniu (włącz instalację z nieznanych źródeł)

### iOS
Już wkrótce...

### Kompilacja ze źródeł
```bash
git clone https://github.com/beniuk1290/edziennik.git
cd edziennik
./gradlew :composeApp:assembleDebug
```

---

## 🔐 Obsługiwane systemy

| System | Logowanie | Status |
|---|---|---|
| **VULCAN stara wersja** | Token + PIN + symbol szkoły | ✅ Pełna obsługa |
| **eduVULCAN (Prometheus) nowe logowanie** | Email + hasło | ✅ Pełna obsługa |
| **Librus Synergia** | Email + hasło | ✅ Podstawowa obsługa |

---

## 📂 Struktura projektu

```
edziennik/
├── composeApp/              # Główna aplikacja (KMP)
│   └── src/
│       ├── commonMain/      # Wspólny kod (API, UI, ViewModels)
│       ├── androidMain/     # Kod Android
│       └── iosMain/         # Kod iOS
├── iosApp/                  # Aplikacja iOS (Swift)
├── .github/                 # GitHub Actions (CI/CD)
├── artwork/                 # Zasoby graficzne
├── docs/                    # Dokumentacja
└── gradle/                  # Gradle wrapper
```

---

## 📖 Dokumentacja

- [Getting started](docs/getting-started.md)
- [Basic usage](docs/basic-usage.md)
- [Logowanie i klient HebeCE](docs/login-and-hebece-client.md)
- [Flow logowania eduVULCAN](docs/eduvulcan-login-flow.md)
- [Prometheus login helper](docs/prometheus-login-helper.md)

---

## 🤝 Wkład

Witamy wszelkie wkłady! Jeśli chcesz pomóc:

1. **Fork** repozytorium
2. Utwórz **branch** ze swoją zmianą (`git checkout -b feature/nowy-funkcjonalnosc`)
3. **Commit** zmiany (`git commit -m 'Dodaj nową funkcjonalność'`)
4. **Push** do brancha (`git push origin feature/nowy-funkcjonalnosc`)
5. Otwórz **Pull Request**

---

## 🐛 Zgłaszanie błędów

Znalazłeś błąd? Otwórz [issue](https://github.com/beniuk1290/edziennik/issues) z opisem:
- Kroków do odtworzenia
- Oczekiwanego zachowania
- Faktycznego zachowania
- Wersji aplikacji i systemu
*(jeszcze nie zrobiłem)*


---

## 📄 Licencja

Projekt jest objęty licencją MIT — zobacz [LICENSE](LICENSE) aby uzyskać więcej informacji.

---

## 🙏 Podziękowania

- **Szkolny.eu** — za użycie kodu do obsługi dziennika Librus

---

<div align="center">

** Jeśli podoba Ci się projekt, daj gwiazdkę na GitHubie! **


</div>
