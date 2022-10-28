import * as Turbo from "@hotwired/turbo";
import "../components/form";
import "../js/navigate";

// opt-in to turbo with `data-turbo="true"`
Turbo.session.drive = false;

// force progress-bar to be visible on frames, too
document.addEventListener("turbo:before-fetch-request", function () {
  Turbo.navigator.delegate.adapter.showProgressBar();
});

document.addEventListener("turbo:frame-render", function (event) {
  Turbo.navigator.delegate.adapter.progressBar.hide();
  if (event.target.getBoundingClientRect().top < 0) {
    event.target.scrollIntoView({ behavior: "smooth" });
  }
});
