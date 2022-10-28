import { setup, cleanup, waitForFinishedJQueryReadyCallbacks } from "../../../../../test/javascript/test-setup-helper";
import fetchMock from "fetch-mock";

describe("calendar", () => {
  const RealDate = Date;
  const dateInstanceIdentifier = Symbol("date-identifier");

  // mocking Date with overridden instanceof operator o_O phew°°°
  // required since datefn verifies with foo instanceof Date
  function mockDate(isoDate) {
    /* eslint-disable unicorn/prevent-abbreviations */

    window.Date = class extends RealDate {
      static [Symbol.hasInstance](instance) {
        return instance[dateInstanceIdentifier];
      }

      static now() {
        return new RealDate(isoDate).getTime();
      }

      // noinspection JSAnnotator
      constructor(...args) {
        // NOSONAR
        let d = args.length === 0 ? new RealDate(isoDate) : new RealDate(...args);
        d[dateInstanceIdentifier] = true;
        return d;
      }
    };
  }

  beforeEach(setup);
  afterEach(cleanup);

  beforeEach(() => {
    window.matchMedia = jest.fn().mockReturnValue({ matches: false, addEventListener: jest.fn() });
  });

  afterEach(() => {
    fetchMock.restore();
    window.Date = RealDate;
  });

  it("renders", async () => {
    // 01.12.2017
    mockDate(1_512_130_448_379);
    await calendarTestSetup();

    renderCalendar(createHolidayService());
    expect(document.body).toMatchSnapshot();
  });

  describe.each([["ALLOWED"], ["WAITING"], ["TEMPORARY_ALLOWED"]])(
    "ensure vacation of status=%s is clickable",
    (givenStatus) => {
      test("in the past", async () => {
        // today is 2017-12-01
        mockDate(1_512_130_448_379);

        // personId -> createHolidayService (param)
        // year -> holidayService.fetchPersonal (param)
        // type -> holidayService.fetchPersonal (implementation detail)
        fetchMock.mock("/persons/42/absences?from=2017-01-01&to=2017-12-31&noWorkdaysInclusive=true", {
          absences: [
            {
              date: "2017-11-01",
              dayLength: 1,
              absencePeriodName: "FULL",
              type: "VACATION",
              status: givenStatus,
              vacationTypeId: 1,
            },
          ],
        });

        await calendarTestSetup();

        const holidayService = createHolidayService({ personId: 42 });
        // fetch personal holiday data and cache the (mocked) response
        // the response is used when the calendar renders
        await holidayService.fetchAbsences(2017);

        renderCalendar(holidayService);

        const $ = document.querySelector.bind(document);
        expect(
          $(
            '[data-datepicker-date="2017-11-01"][data-datepicker-absence-type="VACATION"][data-datepicker-selectable="true"]',
          ),
        ).toBeTruthy();
      });

      test("in the future", async () => {
        // today is 2017-12-01
        mockDate(1_512_130_448_379);

        // personId -> createHolidayService (param)
        // year -> holidayService.fetchPersonal (param)
        // type -> holidayService.fetchPersonal (implementation detail)
        fetchMock.mock("/persons/42/absences?from=2017-01-01&to=2017-12-31&noWorkdaysInclusive=true", {
          absences: [
            {
              date: "2017-12-05",
              dayLength: 1,
              absencePeriodName: "FULL",
              type: "VACATION",
              status: givenStatus,
              vacationTypeId: 1,
            },
          ],
        });

        await calendarTestSetup();

        const holidayService = createHolidayService({ personId: 42 });
        // fetch personal holiday data and cache the (mocked) response
        // the response is used when the calendar renders
        await holidayService.fetchAbsences(2017);

        renderCalendar(holidayService);

        const $ = document.querySelector.bind(document);
        expect(
          $(
            '[data-datepicker-date="2017-12-05"][data-datepicker-absence-type="VACATION"][data-datepicker-selectable="true"]',
          ),
        ).toBeTruthy();
      });

      test("today", async () => {
        // today is 2017-12-01
        mockDate(1_512_130_448_379);

        // personId -> createHolidayService (param)
        // year -> holidayService.fetchPersonal (param)
        // type -> holidayService.fetchPersonal (implementation detail)
        fetchMock.mock("/persons/42/absences?from=2017-01-01&to=2017-12-31&noWorkdaysInclusive=true", {
          absences: [
            {
              date: "2017-12-01",
              dayLength: 1,
              absencePeriodName: "FULL",
              type: "VACATION",
              status: givenStatus,
              vacationTypeId: 1,
            },
          ],
        });

        await calendarTestSetup();

        const holidayService = createHolidayService({ personId: 42 });
        // fetch personal holiday data and cache the (mocked) response
        // the response is used when the calendar renders
        await holidayService.fetchAbsences(2017);

        renderCalendar(holidayService);

        const $ = document.querySelector.bind(document);
        expect(
          $(
            '[data-datepicker-date="2017-12-01"][data-datepicker-absence-type="VACATION"][data-datepicker-selectable="true"]',
          ),
        ).toBeTruthy();
      });
    },
  );

  describe("highlights days", () => {
    test("weekend with absences", async () => {
      // today is 2020-12-13
      mockDate(1_607_848_867_000);

      // personId -> createHolidayService (param)
      // year -> holidayService.fetchPersonal (param)
      // type -> holidayService.fetchPersonal (implementation detail)
      fetchMock.mock("/persons/42/absences?from=2020-01-01&to=2020-12-31&noWorkdaysInclusive=true", {
        absences: [
          {
            date: "2020-12-05",
            absencePeriodName: "FULL",
            type: "VACATION",
            status: "ALLOWED",
            vacationTypeId: 1,
          },
          {
            date: "2020-12-06",
            absencePeriodName: "MORNING",
            type: "VACATION",
            status: "ALLOWED",
            vacationTypeId: 1,
          },
          {
            date: "2020-12-06",
            absencePeriodName: "NOON",
            type: "SICK_NOTE",
            status: "ACTIVE",
          },
          {
            date: "2020-12-12",
            absencePeriodName: "MORNING",
            type: "SICK_NOTE",
            status: "ACTIVE",
          },
          {
            date: "2020-12-12",
            absencePeriodName: "NOON",
            type: "VACATION",
            status: "ALLOWED",
            vacationTypeId: 1,
          },
          {
            date: "2020-12-13",
            absencePeriodName: "FULL",
            type: "SICK_NOTE",
            status: "ACTIVE",
          },
        ],
      });

      await calendarTestSetup();

      const holidayService = createHolidayService({ personId: 42 });
      // fetch personal holiday data and cache the (mocked) response
      // the response is used when the calendar renders
      await holidayService.fetchAbsences(2020);

      renderCalendar(holidayService);

      const $ = document.querySelector.bind(document);
      expect(
        $(
          '[data-datepicker-date="2020-12-05"][class="datepicker-day datepicker-day-weekend datepicker-day-past datepicker-day-absence-full absence-full--solid"]',
        ),
      ).toBeTruthy();
      expect(
        $(
          '[data-datepicker-date="2020-12-06"][class="datepicker-day datepicker-day-weekend datepicker-day-past datepicker-day-absence-morning absence-morning--solid datepicker-day-sick-note-noon"]',
        ),
      ).toBeTruthy();
      expect(
        $(
          '[data-datepicker-date="2020-12-12"][class="datepicker-day datepicker-day-weekend datepicker-day-past datepicker-day-absence-noon absence-noon--solid datepicker-day-sick-note-morning"]',
        ),
      ).toBeTruthy();
      expect(
        $(
          '[data-datepicker-date="2020-12-13"][class="datepicker-day datepicker-day-today datepicker-day-weekend datepicker-day-sick-note-full"]',
        ),
      ).toBeTruthy();
    });
  });

  test("ensure rendering of no-workdays", async () => {
    // today is 2020-12-13
    mockDate(1_607_848_867_000);

    // personId -> createHolidayService (param)
    // year -> holidayService.fetchPersonal (param)
    // type -> holidayService.fetchPersonal (implementation detail)
    fetchMock.mock("/persons/42/absences?from=2020-01-01&to=2020-12-31&noWorkdaysInclusive=true", {
      absences: [
        {
          // saturday
          date: "2020-12-05",
          absencePeriodName: "FULL",
          type: "NO_WORKDAY",
          status: "",
        },
        {
          // sunday
          date: "2020-12-06",
          absencePeriodName: "FULL",
          type: "NO_WORKDAY",
          status: "",
        },
        {
          // wednesday
          date: "2020-12-09",
          absencePeriodName: "FULL",
          type: "NO_WORKDAY",
          status: "",
        },
      ],
    });

    await calendarTestSetup();

    const holidayService = createHolidayService({ personId: 42 });
    // fetch personal holiday data and cache the (mocked) response
    // the response is used when the calendar renders
    await holidayService.fetchAbsences(2020);

    renderCalendar(holidayService);

    const $ = document.querySelector.bind(document);
    expect(
      $('[data-datepicker-date="2020-12-05"][class="datepicker-day datepicker-day-weekend datepicker-day-past"] > svg'),
    ).toBeTruthy();
    expect(
      $('[data-datepicker-date="2020-12-06"][class="datepicker-day datepicker-day-weekend datepicker-day-past"] > svg'),
    ).toBeTruthy();
    expect($('[data-datepicker-date="2020-12-09"][class="datepicker-day datepicker-day-past"] > svg')).toBeTruthy();
  });

  function createHolidayService({ webPrefix = "", apiPrefix = "", personId = 1 } = {}) {
    return window.Urlaubsverwaltung.HolidayService.create(webPrefix, apiPrefix, personId);
  }

  function renderCalendar(holidayService) {
    // note: Date is mocked in calendarTestSetup to return a fixed date value
    const referenceDate = new Date();
    const i18n = (messageKey) => `i18n:${messageKey}`;
    window.Urlaubsverwaltung.Calendar.init(holidayService, referenceDate, i18n);
  }

  async function calendarTestSetup() {
    window.uv = {};
    // 0=sunday, 1=monday
    window.uv.weekStartsOn = 1;

    window.uv.vacationTypes = {};
    window.uv.vacationTypes.colors = {
      1: "#B4D455",
    };

    document.body.innerHTML = `<div id="datepicker"></div>`;

    // loading calendar.js registers a jQuery ready callback
    // which will be executed asynchronously
    await import("./../index.js");

    // therefore we have to wait till ready callbacks are invoked
    return waitForFinishedJQueryReadyCallbacks();
  }
});
