package com.lassanit.extras.classes

import android.app.Activity
import android.view.View
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.Animation.AnimationListener
import android.view.animation.AnimationSet
import android.view.animation.ScaleAnimation
import android.view.animation.TranslateAnimation
import android.view.inputmethod.InputMethodManager
import androidx.annotation.DrawableRes
import com.lassanit.firekit.R

class Utils {
    enum class SignInMethod {
        EMAIL, PHONE, GITHUB, YAHOO, GOOGLE, FACEBOOK, TWITTER, MICROSOFT
    }

    class ProviderData(
        val providerId: String,
        val appName: String,
        @DrawableRes val appRes: Int,
        val scopes: List<String>? = null
    ) {}


    companion object {
        private val map = hashMapOf(
            SignInMethod.EMAIL to ProviderData("password", "Smart Notify", R.drawable.base_email),
            SignInMethod.PHONE to ProviderData("phone", "Phone", R.drawable.logo_phone),
            SignInMethod.GITHUB to ProviderData("github.com", "GitHub", R.drawable.logo_git, listOf("user:email")),
            SignInMethod.YAHOO to ProviderData("yahoo.com", "Yahoo", R.drawable.logo_yahoo),
            SignInMethod.GOOGLE to ProviderData("google.com", "Google", R.drawable.logo_google),
            SignInMethod.FACEBOOK to ProviderData("", "Facebook", R.drawable.logo_fb),
            SignInMethod.TWITTER to ProviderData("twitter.com", "Twitter", R.drawable.logo_twitter),
            SignInMethod.MICROSOFT to ProviderData("microsoft.com", "MicroSoft", R.drawable.logo_microsoft)
        )

        fun getProviderData(method: SignInMethod): ProviderData? {
            return map[method]!!
        }

        fun getProviderData(providerId: String): ProviderData? {
            return when (providerId) {
                "password", "firebase" -> getProviderData(SignInMethod.EMAIL)
                "phone" -> getProviderData(SignInMethod.PHONE)
                "github.com" -> getProviderData(SignInMethod.GITHUB)
                "yahoo.com" -> getProviderData(SignInMethod.YAHOO)
                "google.com" -> getProviderData(SignInMethod.GOOGLE)
                "SignInMethod.FACEBOOK" -> getProviderData(SignInMethod.FACEBOOK)
                "twitter.com" -> getProviderData(SignInMethod.TWITTER)
                "microsoft.com" -> getProviderData(SignInMethod.MICROSOFT)
                else -> null
            }
        }

        fun hideSoftKeyboard(activity: Activity) {
            try {
                val inputMethodManager = activity.getSystemService(
                    Activity.INPUT_METHOD_SERVICE
                ) as InputMethodManager
                inputMethodManager.hideSoftInputFromWindow(
                    activity.currentFocus!!.windowToken,
                    0
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun allCountries(): List<Country> {
            val list = ArrayList<Country>()

            list.add(Country("af", "Afghanistan (‫افغانستان‬‎)", 93))
            list.add(Country("al", "Albania (Shqipëri)", 355))
            list.add(Country("dz", "Algeria (‫الجزائر‬‎)", 213))
            list.add(Country("as", "American Samoa", 1684))
            list.add(Country("ad", "Andorra", 376))
            list.add(Country("ao", "Angola", 244))
            list.add(Country("ai", "Anguilla", 1264))
            list.add(Country("ag", "Antigua and Barbuda", 1268))
            list.add(Country("ar", "Argentina", 54))
            list.add(Country("am", "Armenia (Հայաստան)", 374))
            list.add(Country("aw", "Aruba", 297))
            list.add(Country("au", "Australia", 61))
            list.add(Country("at", "Austria (Österreich)", 43))
            list.add(Country("az", "Azerbaijan (Azərbaycan)", 994))
            list.add(Country("bs", "Bahamas", 1242))
            list.add(Country("bh", "Bahrain (‫البحرين‬‎)", 973))
            list.add(Country("bd", "Bangladesh (বাংলাদেশ)", 880))
            list.add(Country("bb", "Barbados", 1246))
            list.add(Country("by", "Belarus (Беларусь)", 375))
            list.add(Country("be", "Belgium (België)", 32))
            list.add(Country("bz", "Belize", 501))
            list.add(Country("bj", "Benin (Bénin)", 229))
            list.add(Country("bm", "Bermuda", 1441))
            list.add(Country("bt", "Bhutan (འབྲུག)", 975))
            list.add(Country("bo", "Bolivia", 591))
            list.add(Country("ba", "Bosnia and Herzegovina (Босна и Херцеговина)", 387))
            list.add(Country("bw", "Botswana", 267))
            list.add(Country("br", "Brazil (Brasil)", 55))
            list.add(Country("io", "British Indian Ocean Territory", 246))
            list.add(Country("vg", "British Virgin Islands", 1284))
            list.add(Country("bn", "Brunei", 673))
            list.add(Country("bg", "Bulgaria (България)", 359))
            list.add(Country("bf", "Burkina Faso", 226))
            list.add(Country("bi", "Burundi (Uburundi)", 257))
            list.add(Country("kh", "Cambodia (កម្ពុជា)", 855))
            list.add(Country("cm", "Cameroon (Cameroun)", 237))
            list.add(Country("ca", "Canada", 1))
            list.add(Country("cv", "Cape Verde (Kabu Verdi)", 238))
            list.add(Country("bq", "Caribbean Netherlands", 599))
            list.add(Country("ky", "Cayman Islands", 1345))
            list.add(
                Country(
                    "cf",
                    "Central African Republic (République centrafricaine)",
                    236
                )
            )
            list.add(Country("td", "Chad (Tchad)", 235))
            list.add(Country("cl", "Chile", 56))
            list.add(Country("cn", "China (中国)", 86))
            list.add(Country("cx", "Christmas Island", 61))
            list.add(Country("cc", "Cocos (Keeling) Islands", 61))
            list.add(Country("co", "Colombia", 57))
            list.add(Country("km", "Comoros (‫جزر القمر‬‎)", 269))
            list.add(Country("cd", "Congo (DRC) (Jamhuri ya Kidemokrasia ya Kongo)", 243))
            list.add(Country("cg", "Congo (Republic) (Congo-Brazzaville)", 242))
            list.add(Country("ck", "Cook Islands", 682))
            list.add(Country("cr", "Costa Rica", 506))
            list.add(Country("ci", "Côte d’Ivoire", 225))
            list.add(Country("hr", "Croatia (Hrvatska)", 385))
            list.add(Country("cu", "Cuba", 53))
            list.add(Country("cw", "Curaçao", 599))
            list.add(Country("cy", "Cyprus (Κύπρος)", 357))
            list.add(Country("cz", "Czech Republic (Česká republika)", 420))
            list.add(Country("dk", "Denmark (Danmark)", 45))
            list.add(Country("dj", "Djibouti", 253))
            list.add(Country("dm", "Dominica", 1767))
            list.add(Country("do", "Dominican Republic (República Dominicana)", 1))
            list.add(Country("ec", "Ecuador", 593))
            list.add(Country("eg", "Egypt (‫مصر‬‎)", 20))
            list.add(Country("sv", "El Salvador", 503))
            list.add(Country("gq", "Equatorial Guinea (Guinea Ecuatorial)", 240))
            list.add(Country("er", "Eritrea", 291))
            list.add(Country("ee", "Estonia (Eesti)", 372))
            list.add(Country("et", "Ethiopia", 251))
            list.add(Country("fk", "Falkland Islands (Islas Malvinas)", 500))
            list.add(Country("fo", "Faroe Islands (Føroyar)", 298))
            list.add(Country("fj", "Fiji", 679))
            list.add(Country("fi", "Finland (Suomi)", 358))
            list.add(Country("fr", "France", 33))
            list.add(Country("gf", "French Guiana (Guyane française)", 594))
            list.add(Country("pf", "French Polynesia (Polynésie française)", 689))
            list.add(Country("ga", "Gabon", 241))
            list.add(Country("gm", "Gambia", 220))
            list.add(Country("ge", "Georgia (საქართველო)", 995))
            list.add(Country("de", "Germany (Deutschland)", 49))
            list.add(Country("gh", "Ghana (Gaana)", 233))
            list.add(Country("gi", "Gibraltar", 350))
            list.add(Country("gr", "Greece (Ελλάδα)", 30))
            list.add(Country("gl", "Greenland (Kalaallit Nunaat)", 299))
            list.add(Country("gd", "Grenada", 1473))
            list.add(Country("gp", "Guadeloupe", 590))
            list.add(Country("gu", "Guam", 1671))
            list.add(Country("gt", "Guatemala", 502))
            list.add(Country("gg", "Guernsey", 44))
            list.add(Country("gn", "Guinea (Guinée)", 224))
            list.add(Country("gw", "Guinea-Bissau (Guiné Bissau)", 245))
            list.add(Country("gy", "Guyana", 592))
            list.add(Country("ht", "Haiti", 509))
            list.add(Country("hn", "Honduras", 504))
            list.add(Country("hk", "Hong Kong (香港)", 852))
            list.add(Country("hu", "Hungary (Magyarország)", 36))
            list.add(Country("is", "Iceland (Ísland)", 354))
            list.add(Country("in", "India (भारत)", 91))
            list.add(Country("id", "Indonesia", 62))
            list.add(Country("ir", "Iran (‫ایران‬‎)", 98))
            list.add(Country("iq", "Iraq (‫العراق‬‎)", 964))
            list.add(Country("ie", "Ireland", 353))
            list.add(Country("im", "Isle of Man", 44))
            list.add(Country("it", "Italy (Italia)", 39))
            list.add(Country("jm", "Jamaica", 1876))
            list.add(Country("jp", "Japan (日本)", 81))
            list.add(Country("je", "Jersey", 44))
            list.add(Country("jo", "Jordan (‫الأردن‬‎)", 962))
            list.add(Country("kz", "Kazakhstan (Казахстан)", 7))
            list.add(Country("ke", "Kenya", 254))
            list.add(Country("ki", "Kiribati", 686))
            list.add(Country("kw", "Kuwait (‫الكويت‬‎)", 965))
            list.add(Country("kg", "Kyrgyzstan (Кыргызстан)", 996))
            list.add(Country("la", "Laos (ລາວ)", 856))
            list.add(Country("lv", "Latvia (Latvija)", 371))
            list.add(Country("lb", "Lebanon (‫لبنان‬‎)", 961))
            list.add(Country("ls", "Lesotho", 266))
            list.add(Country("lr", "Liberia", 231))
            list.add(Country("ly", "Libya (‫ليبيا‬‎)", 218))
            list.add(Country("li", "Liechtenstein", 423))
            list.add(Country("lt", "Lithuania (Lietuva)", 370))
            list.add(Country("lu", "Luxembourg", 352))
            list.add(Country("mo", "Macau (澳門)", 853))
            list.add(Country("mk", "Macedonia (FYROM) (Македонија)", 389))
            list.add(Country("mg", "Madagascar (Madagasikara)", 261))
            list.add(Country("mw", "Malawi", 265))
            list.add(Country("my", "Malaysia", 60))
            list.add(Country("mv", "Maldives", 960))
            list.add(Country("ml", "Mali", 223))
            list.add(Country("mt", "Malta", 356))
            list.add(Country("mh", "Marshall Islands", 692))
            list.add(Country("mq", "Martinique", 596))
            list.add(Country("mr", "Mauritania (‫موريتانيا‬‎)", 222))
            list.add(Country("mu", "Mauritius (Moris)", 230))
            list.add(Country("yt", "Mayotte", 262))
            list.add(Country("mx", "Mexico (México)", 52))
            list.add(Country("fm", "Micronesia", 691))
            list.add(Country("md", "Moldova (Republica Moldova)", 373))
            list.add(Country("mc", "Monaco", 377))
            list.add(Country("mn", "Mongolia (Монгол)", 976))
            list.add(Country("me", "Montenegro (Crna Gora)", 382))
            list.add(Country("ms", "Montserrat", 1664))
            list.add(Country("ma", "Morocco (‫المغرب‬‎)", 212))
            list.add(Country("mz", "Mozambique (Moçambique)", 258))
            list.add(Country("mm", "Myanmar (Burma) (မြန်မာ)", 95))
            list.add(Country("na", "Namibia (Namibië)", 264))
            list.add(Country("nr", "Nauru", 674))
            list.add(Country("np", "Nepal (नेपाल)", 977))
            list.add(Country("nl", "Netherlands (Nederland)", 31))
            list.add(Country("nc", "New Caledonia (Nouvelle-Calédonie)", 687))
            list.add(Country("nz", "New Zealand", 64))
            list.add(Country("ni", "Nicaragua", 505))
            list.add(Country("ne", "Niger (Nijar)", 227))
            list.add(Country("ng", "Nigeria", 234))
            list.add(Country("nu", "Niue", 683))
            list.add(Country("nf", "Norfolk Island", 672))
            list.add(Country("kp", "North Korea (조선 민주주의 인민 공화국)", 850))
            list.add(Country("mp", "Northern Mariana Islands", 1670))
            list.add(Country("no", "Norway (Norge)", 47))
            list.add(Country("om", "Oman (‫عُمان‬‎)", 968))
            list.add(Country("pk", "Pakistan (‫پاکستان‬‎)", 92))
            list.add(Country("pw", "Palau", 680))
            list.add(Country("ps", "Palestine (‫فلسطين‬‎)", 970))
            list.add(Country("pa", "Panama (Panamá)", 507))
            list.add(Country("pg", "Papua New Guinea", 675))
            list.add(Country("py", "Paraguay", 595))
            list.add(Country("pe", "Peru (Perú)", 51))
            list.add(Country("ph", "Philippines", 63))
            list.add(Country("pl", "Poland (Polska)", 48))
            list.add(Country("pt", "Portugal", 351))
            list.add(Country("pr", "Puerto Rico", 1))
            list.add(Country("qa", "Qatar (‫قطر‬‎)", 974))
            list.add(Country("re", "Réunion (La Réunion)", 262))
            list.add(Country("ro", "Romania (România)", 40))
            list.add(Country("ru", "Russia (Россия)", 7))
            list.add(Country("rw", "Rwanda", 250))
            list.add(Country("bl", "Saint Barthélemy (Saint-Barthélemy)", 590))
            list.add(Country("sh", "Saint Helena", 290))
            list.add(Country("kn", "Saint Kitts and Nevis", 1869))
            list.add(Country("lc", "Saint Lucia", 1758))
            list.add(Country("mf", "Saint Martin (Saint-Martin (partie française))", 590))
            list.add(
                Country(
                    "pm",
                    "Saint Pierre and Miquelon (Saint-Pierre-et-Miquelon)",
                    508
                )
            )
            list.add(Country("vc", "Saint Vincent and the Grenadines", 1784))
            list.add(Country("ws", "Samoa", 685))
            list.add(Country("sm", "San Marino", 378))
            list.add(Country("st", "São Tomé and Príncipe (São Tomé e Príncipe)", 239))
            list.add(Country("sa", "Saudi Arabia (‫المملكة العربية السعودية‬‎)", 966))
            list.add(Country("sn", "Senegal (Sénégal)", 221))
            list.add(Country("rs", "Serbia (Србија)", 381))
            list.add(Country("sc", "Seychelles", 248))
            list.add(Country("sl", "Sierra Leone", 232))
            list.add(Country("sg", "Singapore", 65))
            list.add(Country("sx", "Sint Maarten", 1721))
            list.add(Country("sk", "Slovakia (Slovensko)", 421))
            list.add(Country("si", "Slovenia (Slovenija)", 386))
            list.add(Country("sb", "Solomon Islands", 677))
            list.add(Country("so", "Somalia (Soomaaliya)", 252))
            list.add(Country("za", "South Africa", 27))
            list.add(Country("kr", "South Korea (대한민국)", 82))
            list.add(Country("ss", "South Sudan (‫جنوب السودان‬‎)", 211))
            list.add(Country("es", "Spain (España)", 34))
            list.add(Country("lk", "Sri Lanka (ශ්‍රී ලංකාව)", 94))
            list.add(Country("sd", "Sudan (‫السودان‬‎)", 249))
            list.add(Country("sr", "Suriname", 597))
            list.add(Country("sj", "Svalbard and Jan Mayen", 47))
            list.add(Country("sz", "Swaziland", 268))
            list.add(Country("se", "Sweden (Sverige)", 46))
            list.add(Country("ch", "Switzerland (Schweiz)", 41))
            list.add(Country("sy", "Syria (‫سوريا‬‎)", 963))
            list.add(Country("tw", "Taiwan (台灣)", 886))
            list.add(Country("tj", "Tajikistan", 992))
            list.add(Country("tz", "Tanzania", 255))
            list.add(Country("th", "Thailand (ไทย)", 66))
            list.add(Country("tl", "Timor-Leste", 670))
            list.add(Country("tg", "Togo", 228))
            list.add(Country("tk", "Tokelau", 690))
            list.add(Country("to", "Tonga", 676))
            list.add(Country("tt", "Trinidad and Tobago", 1868))
            list.add(Country("tn", "Tunisia (‫تونس‬‎)", 216))
            list.add(Country("tr", "Turkey (Türkiye)", 90))
            list.add(Country("tm", "Turkmenistan", 993))
            list.add(Country("tc", "Turks and Caicos Islands", 1649))
            list.add(Country("tv", "Tuvalu", 688))
            list.add(Country("vi", "U.S. Virgin Islands", 1340))
            list.add(Country("ug", "Uganda", 256))
            list.add(Country("ua", "Ukraine (Україна)", 380))
            list.add(Country("ae", "United Arab Emirates (‫الإمارات العربية المتحدة‬‎)", 971))
            list.add(Country("gb", "United Kingdom", 44))
            list.add(Country("us", "United States", 1))
            list.add(Country("uy", "Uruguay", 598))
            list.add(Country("uz", "Uzbekistan (Oʻzbekiston)", 998))
            list.add(Country("vu", "Vanuatu", 678))
            list.add(Country("va", "Vatican City (Città del Vaticano)", 39))
            list.add(Country("ve", "Venezuela", 58))
            list.add(Country("vn", "Vietnam (Việt Nam)", 84))
            list.add(Country("wf", "Wallis and Futuna", 681))
            list.add(Country("eh", "Western Sahara (‫الصحراء الغربية‬‎)", 212))
            list.add(Country("ye", "Yemen (‫اليمن‬‎)", 967))
            list.add(Country("zm", "Zambia", 260))
            list.add(Country("zw", "Zimbabwe", 263))
            list.add(Country("ax", "Åland Islands", 358))
            return list
        }

        fun animateViewGone(view: View) {
            val originalWidth = view.width
            val originalHeight = view.height

            val scaleAnimation = getScaleAnimation(false, 300, null) {
                view.visibility = View.INVISIBLE
                view.layoutParams.width = originalWidth
                view.layoutParams.height = originalHeight
            }

            view.startAnimation(scaleAnimation)
        }

        fun animateViewVisible(view: View) {
            view.visibility =
                View.VISIBLE // Set view visibility to VISIBLE before starting the animation

            val originalWidth = view.width
            val originalHeight = view.height


            val scaleAnimation = getScaleAnimation(false, 300, null) {
                view.clearAnimation()
                view.layoutParams.width = originalWidth // Restore original width
                view.layoutParams.height = originalHeight // Restore original height
            }
            view.startAnimation(scaleAnimation)
        }

        fun animateViewMoveDown(view: View, targetY: Float) {
            val originalY = view.y

            val translateAnimation = getMoveAnimation(targetY - originalY, 300, null) {
                view.clearAnimation()
                view.y = targetY // Set the view's final position
            }
            view.startAnimation(translateAnimation)
        }

        fun animateViewMoveUp(view: View, targetY: Float) {
            val originalY = view.y
            val translateAnimation = getMoveAnimation(originalY - targetY, 300, null) {
                view.clearAnimation()
                view.y = targetY // Set the view's final position
            }
            view.startAnimation(translateAnimation)
        }

        fun getMoveAnimation(
            targetY: Float,
            delay: Long = 500,
            start: Runnable? = null,
            end: Runnable? = null,
        ): Animation {
            return TranslateAnimation(
                0f, 0f,  // No horizontal movement
                0f, targetY  // Vertical movement to the target Y position
            ).apply {
                duration = delay   // Duration of the animation in milliseconds
                fillAfter = true // Keeps the final position after the animation ends
                setAnimationListener(object : AnimationListener {
                    override fun onAnimationStart(animation: Animation?) {
                        // Animation start event
                        start?.run()
                    }

                    override fun onAnimationEnd(animation: Animation?) {
                        end?.run()
                    }

                    override fun onAnimationRepeat(animation: Animation?) {
                        // Animation repeat event
                    }
                })
            }

        }

        fun getMoveAnimation(
            from: Float,
            to: Float,
            delay: Long = 500,
            start: Runnable? = null,
            end: Runnable? = null,
        ): Animation {
            return TranslateAnimation(
                0f, 0f,  // No horizontal movement
                from, to  // Vertical movement to the target Y position
            ).apply {
                duration = delay   // Duration of the animation in milliseconds
                fillAfter = true // Keeps the final position after the animation ends
                setAnimationListener(object : AnimationListener {
                    override fun onAnimationStart(animation: Animation?) {
                        // Animation start event
                        start?.run()
                    }

                    override fun onAnimationEnd(animation: Animation?) {
                        end?.run()
                    }

                    override fun onAnimationRepeat(animation: Animation?) {
                        // Animation repeat event
                    }
                })
            }

        }

        fun getFadeMoveAnimation(
            targetY: Float,
            delay: Long = 500,
            start: Runnable? = null,
            end: Runnable? = null,
        ): Animation {
            return TranslateAnimation(
                0f, 0f,  // No horizontal movement
                0f, targetY  // Vertical movement to the target Y position
            ).apply {
                duration = delay   // Duration of the animation in milliseconds
                fillAfter = true // Keeps the final position after the animation ends
                setAnimationListener(object : AnimationListener {
                    override fun onAnimationStart(animation: Animation?) {
                        // Animation start event
                        start?.run()
                    }

                    override fun onAnimationEnd(animation: Animation?) {
                        end?.run()
                    }

                    override fun onAnimationRepeat(animation: Animation?) {
                        // Animation repeat event
                    }
                })
            }

        }

        fun getFade(
            show: Boolean = true, delay: Long = 500, start: Runnable? = null,
            end: Runnable? = null,
        ): Animation {
            // Create the alpha animation for fading

            val fadeAnimation = if (show) AlphaAnimation(0f, 1f) else AlphaAnimation(1f, 0f)
            fadeAnimation.duration = delay // Duration of the fade animation in milliseconds

            fadeAnimation.apply {
                duration = delay // Duration of the animation in milliseconds
                fillAfter = true
                setAnimationListener(object : AnimationListener {
                    override fun onAnimationStart(animation: Animation?) {
                        start?.run()
                    }

                    override fun onAnimationEnd(animation: Animation?) {
                        end?.run()
                    }

                    override fun onAnimationRepeat(animation: Animation?) {
                        // Animation repeat event
                    }
                })
            }
            return fadeAnimation
        }

        fun getFadeMove(delay: Long = 500): AnimationSet {
            // Create the alpha animation for fading
            // Create the translate animation for movement
            val translateAnimation = TranslateAnimation(0f, 0f, -200f, 0f)
            translateAnimation.duration =
                delay // Duration of the translate animation in milliseconds

            // Create an animation set to combine the fading and movement animations
            val animationSet = AnimationSet(true)
            animationSet.addAnimation(getFade(true, delay))
            animationSet.addAnimation(translateAnimation)
            return animationSet
        }

        fun getScaleAnimation(
            appear: Boolean = true,
            delay: Long = 500,
            start: Runnable? = null,
            end: Runnable? = null,
        ): Animation {
            val fX = if (appear) 0f else 1f
            val tX = if (appear) 1f else 0f
            val fY = if (appear) 0f else 1f
            val tY = if (appear) 1f else 0f
            return ScaleAnimation(
                fX, tX, // From 0% scale to 100% scale on X-axis
                fY, tY, // From 0% scale to 100% scale on Y-axis
                Animation.RELATIVE_TO_SELF, 0.5f, // Pivot point for X-axis scaling (center)
                Animation.RELATIVE_TO_SELF, 0.5f // Pivot point for Y-axis scaling (center)
            ).apply {
                duration = delay // Duration of the animation in milliseconds
                fillAfter = true
                setAnimationListener(object : AnimationListener {
                    override fun onAnimationStart(animation: Animation?) {
                        start?.run()
                    }

                    override fun onAnimationEnd(animation: Animation?) {
                        end?.run()
                    }

                    override fun onAnimationRepeat(animation: Animation?) {
                        // Animation repeat event
                    }
                })
            }
        }
    }
}