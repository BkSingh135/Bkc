package com.bkc

enum class AppEnvironment {
    SANDBOX {
        override fun merchant_Key(): String {
            return "oZ7oo9"
        }

        override fun merchant_ID(): String {
            return "6781051"
        }

        override fun furl(): String {
            return "https://payuresponse.firebaseapp.com/failure"
        }

        override fun surl(): String {
            return "https://payuresponse.firebaseapp.com/success"
        }

        override fun salt(): String {
            return "UkojH5TS"
        }

        override fun debug(): Boolean {
            return true
        }
    },
    PRODUCTION {
        override fun merchant_Key(): String {
            return "rSVW2aII"
        }

        override fun merchant_ID(): String {
            return "6781051"
        }

        override fun furl(): String {
            return "https://payuresponse.firebaseapp.com/failure"
        }

        override fun surl(): String {
            return "https://payuresponse.firebaseapp.com/success"
        }

        override fun salt(): String {
            return "UTSy53OQ"
        }

        override fun debug(): Boolean {
            return false
        }
    };

    abstract fun merchant_Key(): String?
    abstract fun merchant_ID(): String?
    abstract fun furl(): String?
    abstract fun surl(): String?
    abstract fun salt(): String?
    abstract fun debug(): Boolean
}