document.addEventListener("DOMContentLoaded", () => {

    const radios = document.querySelectorAll("input[name='answer']");
    const clearBtn = document.getElementById("clearBtn");

    const currentIndex = document.querySelector("input[name='index']").value;
    const paletteBtn = document.querySelector(
        `.palette a[data-index='${currentIndex}']`
    );

	radios.forEach(radio => {
	    radio.addEventListener("change", () => {
	        updatePalette("attempted");
	    });
	});


    // ✅ When Clear clicked → Visited (Blue)
    if (clearBtn) {
        clearBtn.addEventListener("click", () => {
            updatePalette("visited");
        });
    }

    function updatePalette(status) {
        if (!paletteBtn) return;

        paletteBtn.classList.remove(
            "not_visited", "visited", "attempted", "review"
        );

        paletteBtn.classList.add(status);
    }
});
