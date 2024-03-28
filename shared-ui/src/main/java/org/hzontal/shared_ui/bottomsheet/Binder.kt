package org.hzontal.shared_ui.bottomsheet

interface Binder<T : PageHolder> {
        fun onBind(holder: T)
    }


