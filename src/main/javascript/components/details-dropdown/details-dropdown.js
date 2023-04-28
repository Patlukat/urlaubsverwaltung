import { onTurboBeforeRenderRestore } from "../../js/turbo";

// there has to be made some considerations for window.history handling (navigating backwards)
// use case:
// - web page renders with closed `<details-dropdown>`
// - user opens the dropdown
// - user changes form data within the dropdown and submits the form
//   - response received
//   - current page snapshot is created (cache for client side history navigation)
//   - history.pushState() is invoked
//   - new page will be rendered
// - user navigates back with history.back()
//
// client side routing (with `hotwire/turbo` in our case) results in:
// - rendering the cached page.
//   this cached page includes the opened `<details-dropdown>` instead of the initially closed element.
//
// -> however, navigating backwards we want to render the initial element. the closed one.
//
onTurboBeforeRenderRestore(function (event) {
  // close all dropdowns
  for (let dropdown of event.detail.newBody.querySelectorAll("[is=uv-details-dropdown]")) {
    dropdown.open = false;
  }
});

class DetailsDropdown extends HTMLDetailsElement {
  connectedCallback() {
    let contentClicked = false;

    const handleDocumentClick = (event) => {
      const { target } = event;
      contentClicked = this.contains(target);
      if (contentClicked) {
        if (!isInteractiveElement(target)) {
          // prevent closing of detail element only when clicked element is not interactive.
          // otherwise we would prevent a form submit for instance.
          event.preventDefault();
        }
        if (target.tagName === "SUMMARY" || target.closest("summary")) {
          this.open = !this.open;
        }
      } else if (target !== this) {
        this.open = false;
      }
    };

    const handleDocumentKeyUp = (event) => {
      if (event.key === "Escape") {
        this.open = false;
        contentClicked = false;
      }
    };

    const handleFocusOut = (event) => {
      if (event.target.tagName !== "SUMMARY") {
        setTimeout(() => {
          if (!this.matches(":focus-within") && (document.activeElement !== document.body || !contentClicked)) {
            this.open = false;
          }
        }, Number(this.dataset.closeDelay ?? 0));
      }
    };

    document.addEventListener("click", handleDocumentClick);
    document.addEventListener("keyup", handleDocumentKeyUp);
    this.addEventListener("focusout", handleFocusOut);

    this.cleanup = () => {
      document.removeEventListener("click", handleDocumentClick);
      document.removeEventListener("keyup", handleDocumentKeyUp);
      this.removeEventListener("focusout", handleFocusOut);
    };
  }

  disconnectedCallback() {
    this.cleanup();
  }
}

const interactiveElements = [HTMLButtonElement, HTMLInputElement, HTMLSelectElement, HTMLTextAreaElement];

function isInteractiveElement(element) {
  return interactiveElements.some((ElementType) => element instanceof ElementType);
}

customElements.define("uv-details-dropdown", DetailsDropdown, { extends: "details" });
