import { endOfMonth, formatISO, isToday, isWeekend, parseISO } from "date-fns";
import parse from "../../lib/date-fns/parse";
import { defineCustomElements } from "@duetds/date-picker/dist/loader";
import { getJSON } from "../../js/fetch";
import { createDatepickerLocalization } from "./locale";
import {
  addAbsenceTypeStyleToNode,
  isPersonalHolidayApprovedFull,
  isPersonalHolidayApprovedMorning,
  isPersonalHolidayApprovedNoon,
  isPersonalHolidayTemporaryFull,
  isPersonalHolidayTemporaryMorning,
  isPersonalHolidayTemporaryNoon,
  isPersonalHolidayWaitingFull,
  isPersonalHolidayWaitingMorning,
  isPersonalHolidayWaitingNoon,
  isSickNoteFull,
  isSickNoteMorning,
  isSickNoteNoon,
  isNoWorkday,
  removeAbsenceTypeStyleFromNode,
} from "../../js/absence";
import { isPublicHoliday, isPublicHolidayMorning, isPublicHolidayNoon } from "../../js/public-holiday";
import "@duetds/date-picker/dist/collection/themes/default.css";
import "./datepicker.css";
import "../calendar/calendar.css";

// register @duet/datepicker
defineCustomElements(window);

const noop = () => {};

const datepickerClassnames = {
  day: "datepicker-day",
  today: "datepicker-day-today",
  past: "datepicker-day-past",
  weekend: "datepicker-day-weekend",
  publicHolidayFull: "datepicker-day-public-holiday-full",
  publicHolidayMorning: "datepicker-day-public-holiday-morning",
  publicHolidayNoon: "datepicker-day-public-holiday-noon",
  personalHolidayFull: "datepicker-day-personal-holiday-full",
  personalHolidayFullApproved: "datepicker-day-personal-holiday-full-approved",
  personalHolidayMorning: "datepicker-day-personal-holiday-morning",
  personalHolidayMorningApproved: "datepicker-day-personal-holiday-morning-approved",
  personalHolidayNoon: "datepicker-day-personal-holiday-noon",
  personalHolidayNoonApproved: "datepicker-day-personal-holiday-noon-approved",
  sickNoteFull: "datepicker-day-sick-note-full",
  sickNoteMorning: "datepicker-day-sick-note-morning",
  sickNoteNoon: "datepicker-day-sick-note-noon",
};

export async function createDatepicker(selector, { urlPrefix, getPersonId, onSelect = noop }) {
  const { localisation } = window.uv.datepicker;
  const { dateAdapter, dateFormatShort } = createDatepickerLocalization({ locale: localisation.locale });

  const duetDateElement = await replaceNativeDateInputWithDuetDatePicker(selector, dateAdapter, localisation);

  const monthElement = duetDateElement.querySelector(".duet-date__select--month");
  const yearElement = duetDateElement.querySelector(".duet-date__select--year");

  const showAbsences = () => {
    // clear all days
    for (const element of duetDateElement.querySelectorAll(".duet-date__day")) {
      element.classList.remove(...Object.values(datepickerClassnames));
      element.querySelector("[data-uv-icon]")?.remove();
      removeAbsenceTypeStyleFromNode(element);
    }

    const firstDayOfMonth = `${yearElement.value}-${twoDigit(Number(monthElement.value) + 1)}-01`;
    const lastDayOfMonth = formatISO(endOfMonth(parseISO(firstDayOfMonth)), { representation: "date" });

    const personId = getPersonId();
    if (!personId) {
      return;
    }

    Promise.allSettled([
      getJSON(`${urlPrefix}/persons/${personId}/public-holidays?from=${firstDayOfMonth}&to=${lastDayOfMonth}`).then(
        pick("publicHolidays"),
      ),
      getJSON(
        `${urlPrefix}/persons/${personId}/absences?from=${firstDayOfMonth}&to=${lastDayOfMonth}&noWorkdaysInclusive=true`,
      ).then(pick("absences")),
    ]).then(([publicHolidays, absences]) => {
      const selectedMonth = Number(monthElement.value);
      const selectedYear = Number(yearElement.value);
      for (let dayElement of duetDateElement.querySelectorAll(".duet-date__day")) {
        const dayAndMonthString = dayElement.querySelector(".duet-date__vhidden").textContent;
        const date = parse(dayAndMonthString, dateFormatShort, new Date());
        // dayAndMonthString is a hard coded duet-date-picker screen-reader-only value which does not contain the year.
        // therefore the parsed date will always be assigned to the current year and we have to adjust it when:
        if (selectedMonth === 0 && date.getMonth() === 11) {
          // datepicker selected month is january, but the rendered day item is december of the previous year
          // (e.g. december 31) to fill the week row.
          date.setFullYear(selectedYear - 1);
        } else if (selectedMonth === 11 && date.getMonth() === 0) {
          // datepicker selected month is december, but the rendered day item is january of the next year
          // (e.g. january 1) to fill the week row.
          date.setFullYear(selectedYear + 1);
        } else {
          date.setFullYear(selectedYear);
        }
        const cssClasses = getCssClassesForDate(date, publicHolidays.value, absences.value);
        dayElement.classList.add(...cssClasses);

        const absencesForDate = findByDate(absences.value, date);
        addAbsenceTypeStyleToNode(dayElement, absencesForDate);

        let icon;

        if (isNoWorkday(absencesForDate)) {
          const temporary = document.createElement("span");
          temporary.innerHTML = `<svg viewBox="0 0 20 20" class="tw-w-3 tw-h-3 tw-opacity-50 tw-stroke-2" fill="currentColor" width="16" height="16" role="img" aria-hidden="true" focusable="false"><path fill-rule="evenodd" d="M13.477 14.89A6 6 0 015.11 6.524l8.367 8.368zm1.414-1.414L6.524 5.11a6 6 0 018.367 8.367zM18 10a8 8 0 11-16 0 8 8 0 0116 0z" clip-rule="evenodd"></path></svg>`;
          icon = temporary.firstChild;
        } else {
          icon = document.createElement("span");
          icon.classList.add("tw-w-3", "tw-h-3", "tw-inline-block");
        }

        icon.dataset.uvIcon = "";
        dayElement.append(icon);
      }
    });
  };

  const toggleButton = duetDateElement.querySelector("button.duet-date__toggle");
  toggleButton.addEventListener("click", showAbsences);
  duetDateElement.addEventListener("duetChange", (event) => onSelect(event));

  duetDateElement.querySelector(".duet-date__prev").addEventListener("click", showAbsences);
  duetDateElement.querySelector(".duet-date__next").addEventListener("click", showAbsences);

  monthElement.addEventListener("change", showAbsences);
  yearElement.addEventListener("change", showAbsences);

  return duetDateElement;
}

async function replaceNativeDateInputWithDuetDatePicker(selector, dateAdapter, localization) {
  const dateElement = document.querySelector(selector);
  const duetDateElement = document.createElement("duet-date-picker");

  duetDateElement.dateAdapter = dateAdapter;
  duetDateElement.localization = localization;

  duetDateElement.setAttribute("style", "--duet-radius=0");
  duetDateElement.setAttribute("class", dateElement.getAttribute("class"));
  duetDateElement.setAttribute("value", dateElement.dataset.isoValue || "");
  duetDateElement.setAttribute("identifier", dateElement.getAttribute("id"));

  if (dateElement.dataset.min) {
    duetDateElement.setAttribute("min", dateElement.dataset.min);
  }

  if (dateElement.dataset.max) {
    duetDateElement.setAttribute("max", dateElement.dataset.max);
  }

  dateElement.replaceWith(duetDateElement);

  await waitForDatePickerHydration(duetDateElement);

  // name attribute must be set to the actual visible input element
  // the backend handles the raw user input for progressive enhancement reasons.
  // (german locale is 'dd.MM.yyyy', while english locale would be 'yyyy/MM/dd' for instance)
  const duetDateInputElement = duetDateElement.querySelector("input.duet-date__input");
  duetDateInputElement.setAttribute("name", dateElement.getAttribute("name"));

  for (const [key, value] of Object.entries(dateElement.dataset)) {
    duetDateInputElement.dataset[key] = value;
  }

  return duetDateElement;
}

function waitForDatePickerHydration(rootElement) {
  return new Promise((resolve) => {
    const observer = new MutationObserver((mutationsList) => {
      for (const mutation of mutationsList) {
        if (mutation.target.classList.contains("hydrated")) {
          resolve();
          observer.disconnect();
          return true;
        }
      }
    });
    observer.observe(rootElement, { attributes: true });
  });
}

function dateToString(date) {
  return date ? formatISO(date, { representation: "date" }) : "";
}

const isPast = () => false;

function getCssClassesForDate(date, publicHolidays, absences) {
  const dateString = dateToString(date);
  const absencesForDate = absences.filter((absence) => absence.date === dateString);
  const publicHolidaysForDate = publicHolidays.filter((publicHoliday) => publicHoliday.date === dateString);

  return [
    datepickerClassnames.day,
    isToday(date) && datepickerClassnames.today,
    isPast() && datepickerClassnames.past,
    isWeekend(date) && datepickerClassnames.weekend,
    isPublicHoliday(publicHolidaysForDate) && datepickerClassnames.publicHolidayFull,
    isPublicHolidayMorning(publicHolidaysForDate) && datepickerClassnames.publicHolidayMorning,
    isPublicHolidayNoon(publicHolidaysForDate) && datepickerClassnames.publicHolidayNoon,
    isPersonalHolidayWaitingFull(absencesForDate) && datepickerClassnames.personalHolidayFull,
    isPersonalHolidayTemporaryFull(absencesForDate) && datepickerClassnames.personalHolidayFull,
    isPersonalHolidayApprovedFull(absencesForDate) && datepickerClassnames.personalHolidayFullApproved,
    isPersonalHolidayWaitingMorning(absencesForDate) && datepickerClassnames.personalHolidayMorning,
    isPersonalHolidayTemporaryMorning(absencesForDate) && datepickerClassnames.personalHolidayMorning,
    isPersonalHolidayApprovedMorning(absencesForDate) && datepickerClassnames.personalHolidayMorningApproved,
    isPersonalHolidayWaitingNoon(absencesForDate) && datepickerClassnames.personalHolidayNoon,
    isPersonalHolidayTemporaryNoon(absencesForDate) && datepickerClassnames.personalHolidayNoon,
    isPersonalHolidayApprovedNoon(absencesForDate) && datepickerClassnames.personalHolidayNoonApproved,
    isSickNoteFull(absencesForDate) && datepickerClassnames.sickNoteFull,
    isSickNoteMorning(absencesForDate) && datepickerClassnames.sickNoteMorning,
    isSickNoteNoon(absencesForDate) && datepickerClassnames.sickNoteNoon,
  ].filter(Boolean);
}

function findByDate(list, date) {
  const dateString = dateToString(date);
  return list.filter((item) => item.date === dateString);
}

function pick(name) {
  return function (object) {
    return object[name];
  };
}

function twoDigit(nr) {
  return ("0" + nr).slice(-2);
}
