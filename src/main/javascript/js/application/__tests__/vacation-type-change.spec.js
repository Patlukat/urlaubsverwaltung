import vacationTypeChanged from "../vacation-type-changed";

describe("vacationTypeChanged", function () {
  afterEach(function () {
    // cleanup DOM
    while (document.body.firstElementChild) {
      document.body.firstElementChild.remove();
    }
  });

  describe("SPECIAL_LEAVE", function () {
    it("adds 'hidden' class to 'overtime' element", function () {
      const overtimeElement = document.createElement("div");
      overtimeElement.setAttribute("id", "overtime");
      document.body.append(overtimeElement);

      vacationTypeChanged("2000");

      expect(overtimeElement.classList.contains("hidden")).toBeTruthy();
    });

    it("removes 'hidden' class from 'special-leave' element", function () {
      const specialLeaveElement = document.createElement("div");
      specialLeaveElement.setAttribute("id", "special-leave");
      specialLeaveElement.classList.add("hidden");
      document.body.append(specialLeaveElement);

      vacationTypeChanged("2000");

      expect(specialLeaveElement.classList.contains("hidden")).toBeFalsy();
    });

    it("does not throw when no element exists", function () {
      expect(() => {
        vacationTypeChanged("2000");
      }).not.toThrow();
    });
  });

  describe("OVERTIME", function () {
    it("removes 'hidden' class from 'overtime' element", function () {
      const overtimeElement = document.createElement("div");
      overtimeElement.setAttribute("id", "overtime");
      overtimeElement.classList.add("hidden");
      document.body.append(overtimeElement);

      vacationTypeChanged("4000");

      expect(overtimeElement.classList.contains("hidden")).toBeFalsy();
    });

    it("adds 'hidden' class to 'special-leave' element", function () {
      const specialLeaveElement = document.createElement("div");
      specialLeaveElement.setAttribute("id", "special-leave");
      document.body.append(specialLeaveElement);

      vacationTypeChanged("4000");

      expect(specialLeaveElement.classList.contains("hidden")).toBeTruthy();
    });

    it("does not throw when no element exists", function () {
      expect(() => {
        vacationTypeChanged("4000");
      }).not.toThrow();
    });
  });

  describe("any other value", function () {
    it("adds 'hidden' class to 'overtime' element", function () {
      const overtimeElement = document.createElement("div");
      overtimeElement.setAttribute("id", "overtime");
      document.body.append(overtimeElement);

      vacationTypeChanged("unknown");

      expect(overtimeElement.classList.contains("hidden")).toBeTruthy();
    });

    it("adds 'hidden' class to 'special-leave' element", function () {
      const specialLeaveElement = document.createElement("div");
      specialLeaveElement.setAttribute("id", "special-leave");
      document.body.append(specialLeaveElement);

      vacationTypeChanged("unknown");

      expect(specialLeaveElement.classList.contains("hidden")).toBeTruthy();
    });

    it("does not throw when no element exists", function () {
      expect(() => {
        vacationTypeChanged("unknown");
      }).not.toThrow();
    });
  });
});
