document.addEventListener("DOMContentLoaded", () => {

    function enterFS() {
        const el = document.documentElement;
        if (el.requestFullscreen) el.requestFullscreen();
    }

    enterFS();

    document.addEventListener("fullscreenchange", () => {
        if (!document.fullscreenElement) {
            enterFS();
        }
    });
});

function exitFullscreen() {
    if (document.fullscreenElement) {
        document.exitFullscreen();
    }
}
