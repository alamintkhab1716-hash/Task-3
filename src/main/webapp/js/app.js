/**
 * Soft Nexis Contact Manager - Interactive Client Operations
 * - Real-time form validation
 * - Auto-focus first invalid field
 * - Loading spinner & double-submit prevention
 * - Search-as-you-type contact list filtering
 * - Toast notification dismissal
 */

document.addEventListener("DOMContentLoaded", function () {
    initToasts();
    initContactForm();
    initSearchAsYouType();
});

/* --------------------------------------------------------------------------
   Toast Notifications
   -------------------------------------------------------------------------- */
function initToasts() {
    const toasts = document.querySelectorAll(".toast");
    toasts.forEach(toast => {
        // Auto dismiss after 4 seconds
        setTimeout(() => {
            dismissToast(toast);
        }, 4000);

        const closeBtn = toast.querySelector(".toast-close-btn");
        if (closeBtn) {
            closeBtn.addEventListener("click", () => dismissToast(toast));
        }
    });
}

function dismissToast(toast) {
    if (!toast) return;
    toast.style.opacity = "0";
    toast.style.transform = "translateX(120%)";
    setTimeout(() => {
        if (toast.parentElement) {
            toast.parentElement.removeChild(toast);
        }
    }, 300);
}

/* --------------------------------------------------------------------------
   Contact Form Validation & UX
   -------------------------------------------------------------------------- */
function initContactForm() {
    const form = document.getElementById("contactForm");
    if (!form) return;

    const nameInput = document.getElementById("name");
    const emailInput = document.getElementById("email");
    const phoneInput = document.getElementById("phone");
    const submitBtn = document.getElementById("submitBtn");

    // Real-time validation listeners
    if (nameInput) {
        nameInput.addEventListener("input", () => validateField(nameInput, validateName));
        nameInput.addEventListener("blur", () => validateField(nameInput, validateName));
    }
    if (emailInput) {
        emailInput.addEventListener("input", () => validateField(emailInput, validateEmail));
        emailInput.addEventListener("blur", () => validateField(emailInput, validateEmail));
    }
    if (phoneInput) {
        phoneInput.addEventListener("input", () => validateField(phoneInput, validatePhone));
        phoneInput.addEventListener("blur", () => validateField(phoneInput, validatePhone));
    }

    // Auto-focus first invalid field or name input on page load
    const serverFirstInvalid = form.getAttribute("data-first-invalid");
    if (serverFirstInvalid) {
        const fieldToFocus = document.getElementById(serverFirstInvalid);
        if (fieldToFocus) {
            fieldToFocus.focus();
        }
    } else if (nameInput && !nameInput.value) {
        nameInput.focus();
    }

    // Form submit listener
    form.addEventListener("submit", function (e) {
        let isValid = true;
        let firstInvalidElement = null;

        const nameValid = validateField(nameInput, validateName);
        if (!nameValid) {
            isValid = false;
            firstInvalidElement = firstInvalidElement || nameInput;
        }

        const emailValid = validateField(emailInput, validateEmail);
        if (!emailValid) {
            isValid = false;
            firstInvalidElement = firstInvalidElement || emailInput;
        }

        const phoneValid = validateField(phoneInput, validatePhone);
        if (!phoneValid) {
            isValid = false;
            firstInvalidElement = firstInvalidElement || phoneInput;
        }

        if (!isValid) {
            e.preventDefault();
            if (firstInvalidElement) {
                firstInvalidElement.focus();
            }
            return false;
        }

        // Prevent duplicate submissions & show loading spinner
        if (submitBtn) {
            submitBtn.disabled = true;
            submitBtn.classList.add("loading");
            const btnText = submitBtn.querySelector(".btn-text");
            if (btnText) {
                btnText.textContent = "Saving Contact...";
            }
        }
    });
}

function validateField(input, validationFn) {
    if (!input) return true;
    const value = input.value.trim();
    const result = validationFn(value);
    const errorEl = document.getElementById(input.id + "-error");

    if (result.isValid) {
        input.classList.remove("is-invalid");
        input.classList.add("is-valid");
        if (errorEl) {
            errorEl.textContent = "";
            errorEl.style.display = "none";
        }
        return true;
    } else {
        input.classList.remove("is-valid");
        input.classList.add("is-invalid");
        if (errorEl) {
            errorEl.textContent = result.message;
            errorEl.style.display = "flex";
        }
        return false;
    }
}

function validateName(value) {
    if (!value || value.length < 2 || value.length > 50) {
        return { isValid: false, message: "Please enter valid name" };
    }
    return { isValid: true };
}

function validateEmail(value) {
    const emailPattern = /^[A-Za-z0-9+_.-]+@[A-Za-z0-9]+([.-][A-Za-z0-9]+)*\.[A-Za-z]{2,}$/;
    if (!value || !emailPattern.test(value)) {
        return { isValid: false, message: "Invalid email address" };
    }
    return { isValid: true };
}

function validatePhone(value) {
    if (!value || value.length === 0) {
        // Phone is optional
        return { isValid: true };
    }
    const digitsOnly = value.replace(/[^0-9]/g, "");
    const phonePattern = /^(\d{10}|\d{3}[-.\s]\d{3}[-.\s]\d{4}|\(\d{3}\)\s*\d{3}[-.\s]\d{4})$/;
    if (!phonePattern.test(value) || digitsOnly.length !== 10) {
        return { isValid: false, message: "Use 10-digit format" };
    }
    return { isValid: true };
}

/* --------------------------------------------------------------------------
   Search-as-you-type Contact List Filter
   -------------------------------------------------------------------------- */
function initSearchAsYouType() {
    const searchInput = document.getElementById("searchContactsInput");
    const clearBtn = document.getElementById("searchClearBtn");
    const tableBody = document.getElementById("contactsTableBody");
    const countBadge = document.getElementById("filteredCountBadge");
    const emptyRow = document.getElementById("noFilterResultsRow");

    if (!searchInput || !tableBody) return;

    searchInput.addEventListener("input", function () {
        const filter = searchInput.value.toLowerCase().trim();
        const rows = tableBody.querySelectorAll("tr.contact-row");

        if (clearBtn) {
            clearBtn.style.display = filter.length > 0 ? "block" : "none";
        }

        let visibleCount = 0;

        rows.forEach(row => {
            const name = (row.getAttribute("data-name") || "").toLowerCase();
            const email = (row.getAttribute("data-email") || "").toLowerCase();
            const phone = (row.getAttribute("data-phone") || "").toLowerCase();

            if (name.includes(filter) || email.includes(filter) || phone.includes(filter)) {
                row.style.display = "";
                visibleCount++;
            } else {
                row.style.display = "none";
            }
        });

        if (countBadge) {
            countBadge.textContent = visibleCount;
        }

        if (emptyRow) {
            emptyRow.style.display = (visibleCount === 0 && rows.length > 0) ? "" : "none";
        }
    });

    if (clearBtn) {
        clearBtn.addEventListener("click", function () {
            searchInput.value = "";
            searchInput.dispatchEvent(new Event("input"));
            searchInput.focus();
        });
    }
}
