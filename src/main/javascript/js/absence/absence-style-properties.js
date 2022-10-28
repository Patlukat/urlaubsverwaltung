import {
  isPersonalHolidayApprovedFull,
  isPersonalHolidayApprovedMorning,
  isPersonalHolidayApprovedNoon,
  isPersonalHolidayTemporaryFull,
  isPersonalHolidayTemporaryMorning,
  isPersonalHolidayTemporaryNoon,
  isPersonalHolidayWaitingFull,
  isPersonalHolidayWaitingMorning,
  isPersonalHolidayWaitingNoon,
} from "./absence-assertions";

const property = (n, v) => ({ name: n, value: v });
const propertyMorning = (v) => property("--absence-bar-color-morning", `var(--absence-color-${v})`);
const propertyNoon = (v) => property("--absence-bar-color-noon", `var(--absence-color-${v})`);
const propertyFull = (v) => property("--absence-bar-color", `var(--absence-color-${v})`);

/**
 *
 * @param node {HTMLElement}
 * @param absences {Array}
 */
export function addAbsenceTypeStyleToNode(node, absences) {
  const properties = getStylePropertiesForDate(absences);
  for (let { name, value } of properties) {
    node.style.setProperty(name, value);
  }
}

/**
 *
 * @param node {HTMLElement}
 */
export function removeAbsenceTypeStyleFromNode(node) {
  node.style.removeProperty("--absence-bar-color-morning");
  node.style.removeProperty("--absence-bar-color-noon");
  node.style.removeProperty("--absence-bar-color");
}

/**
 *
 * @param absences {Array}
 * @returns {{name, value}[]}
 */
function getStylePropertiesForDate(absences) {
  const [colorMorningOrFull, colorNoon] = getVacationTypeColors(absences);
  return [
    isPersonalHolidayWaitingFull(absences) ? propertyFull(colorMorningOrFull) : undefined,
    isPersonalHolidayTemporaryFull(absences) ? propertyFull(colorMorningOrFull) : undefined,
    isPersonalHolidayApprovedFull(absences) ? propertyFull(colorMorningOrFull) : undefined,
    isPersonalHolidayWaitingMorning(absences) ? propertyMorning(colorMorningOrFull) : undefined,
    isPersonalHolidayTemporaryMorning(absences) ? propertyMorning(colorMorningOrFull) : undefined,
    isPersonalHolidayApprovedMorning(absences) ? propertyMorning(colorMorningOrFull) : undefined,
    isPersonalHolidayWaitingNoon(absences) ? propertyNoon(colorNoon) : undefined,
    isPersonalHolidayTemporaryNoon(absences) ? propertyNoon(colorNoon) : undefined,
    isPersonalHolidayApprovedNoon(absences) ? propertyNoon(colorNoon) : undefined,
  ].filter(Boolean);
}

/**
 *
 * @param absences {Array}
 * @returns {string[]}
 */
function getVacationTypeColors(absences) {
  let vacationTypeIdMorningOrFull;
  let vacationTypeIdNoon;

  for (let absence of absences) {
    if (absence.absencePeriodName === "FULL" || absence.absencePeriodName === "MORNING") {
      vacationTypeIdMorningOrFull = absence.vacationTypeId;
    }
    if (absence.absencePeriodName === "NOON") {
      vacationTypeIdNoon = absence.vacationTypeId;
    }
  }

  const colorMorningOrFull = window.uv.vacationTypes.colors[vacationTypeIdMorningOrFull];
  const colorNoon = window.uv.vacationTypes.colors[vacationTypeIdNoon];

  return [colorMorningOrFull, colorNoon];
}
