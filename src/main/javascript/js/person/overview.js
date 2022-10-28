import $ from "jquery";
import { getYear, setYear, startOfYear, subMonths, addMonths } from "date-fns";

import getUrlParameter from "../get-url-parameter";
import "../../components/calendar";

$(document).ready(function () {
  const personId = window.uv.personId;
  const webPrefix = window.uv.webPrefix;
  const apiPrefix = window.uv.apiPrefix;

  function i18n(messageKey) {
    return window.uv.i18n[messageKey] || `/i18n:${messageKey}/`;
  }

  function initCalendar() {
    const year = getUrlParameter("year");
    let date = new Date();

    if (year.length > 0 && year != getYear(date)) {
      date = startOfYear(setYear(date, year));
    }

    const holidayService = Urlaubsverwaltung.HolidayService.create(webPrefix, apiPrefix, +personId);

    const shownNumberOfMonths = 10;
    const startDate = subMonths(date, shownNumberOfMonths / 2);
    const endDate = addMonths(date, shownNumberOfMonths / 2);

    const yearOfStartDate = getYear(startDate);
    const yearOfEndDate = getYear(endDate);

    // TODO Performance reduce calls when yearOfStartDate === yearOfEndDate
    $.when(
      holidayService.fetchPublic(yearOfStartDate),
      holidayService.fetchAbsences(yearOfStartDate),

      holidayService.fetchPublic(yearOfEndDate),
      holidayService.fetchAbsences(yearOfEndDate),
    ).always(function () {
      Urlaubsverwaltung.Calendar.init(holidayService, date, i18n);
    });
  }

  initCalendar();
});
