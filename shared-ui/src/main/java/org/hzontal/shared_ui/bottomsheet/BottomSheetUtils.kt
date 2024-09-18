package org.hzontal.shared_ui.bottomsheet

import android.app.Activity
import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.AppCompatRadioButton
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.progressindicator.LinearProgressIndicator
import org.hzontal.shared_ui.R
import org.hzontal.shared_ui.appbar.ToolbarComponent
import org.hzontal.shared_ui.buttons.RoundButton
import org.hzontal.shared_ui.extensions.setProgressPercent
import org.hzontal.shared_ui.utils.DialogUtils
import java.util.concurrent.atomic.AtomicInteger

object BottomSheetUtils {

    const val LONG_TIMEOUT: Long = 3000
    const val SHORT_TIMEOUT: Long = 1500

    @JvmStatic
    fun showStandardSheet(
        fragmentManager: FragmentManager,
        titleText: String?,
        descriptionText: String?,
        actionButtonLabel: String? = null,
        cancelButtonLabel: String? = null,
        onConfirmClick: (() -> Unit)? = null,
        onCancelClick: (() -> Unit)? = null
    ) {

        val customSheetFragment =
            CustomBottomSheetFragment.with(fragmentManager).page(R.layout.standar_sheet_layout)
                .cancellable(true)
        customSheetFragment.holder(GenericSheetHolder(), object : Binder<GenericSheetHolder> {
            override fun onBind(holder: GenericSheetHolder) {
                with(holder) {
                    title.text = titleText
                    description.text = descriptionText
                    actionButtonLabel?.let {
                        actionButton.text = it
                    }
                    cancelButtonLabel?.let {
                        cancelButton.text = it
                    }

                    actionButton.setOnClickListener {
                        onConfirmClick?.invoke()
                        customSheetFragment.dismiss()
                    }

                    cancelButton.setOnClickListener {
                        onCancelClick?.invoke()
                        customSheetFragment.dismiss()
                    }

                    actionButton.visibility =
                        if (actionButtonLabel.isNullOrEmpty()) View.GONE else View.VISIBLE

                }
            }
        })

        customSheetFragment.transparentBackground()
        customSheetFragment.launch()
    }

    class GenericSheetHolder : PageHolder() {
        lateinit var actionButton: TextView
        lateinit var cancelButton: TextView
        lateinit var title: TextView
        lateinit var description: TextView

        override fun bindView(view: View) {
            actionButton = view.findViewById(R.id.standard_sheet_confirm_btn)
            cancelButton = view.findViewById(R.id.standard_sheet_cancel_btn)
            title = view.findViewById(R.id.standard_sheet_title)
            description = view.findViewById(R.id.standard_sheet_content)
        }
    }

    interface LockOptionConsumer {
        fun accept(option: Long)
    }

    fun showRadioListSheet(
        fragmentManager: FragmentManager,
        context: Context,
        currentValue: Long,
        radioList: LinkedHashMap<Long, Int>,
        titleText: String?,
        descriptionText: String?,
        actionButtonLabel: String? = null,
        cancelButtonLabel: String? = null,
        consumer: Any
    ) {

        val customSheetFragment =
            CustomBottomSheetFragment.with(fragmentManager).page(R.layout.radio_list_sheet_layout)
                .cancellable(true)
        customSheetFragment.holder(RadioListSheetHolder(), object : Binder<RadioListSheetHolder> {
            override fun onBind(holder: RadioListSheetHolder) {
                with(holder) {
                    title.text = titleText
                    description.text = descriptionText
                    actionButtonLabel?.let {
                        actionButton.text = it
                    }
                    cancelButtonLabel?.let {
                        cancelButton.text = it
                    }

                    actionButton.setOnClickListener {
                        val radioButton: AppCompatRadioButton =
                            radioGroup.findViewById(radioGroup.checkedRadioButtonId)
                        val option = radioButton.tag as Long
                        (consumer as LockOptionConsumer).accept(option)
                        customSheetFragment.dismiss()
                    }

                    cancelButton.setOnClickListener {
                        customSheetFragment.dismiss()
                    }

                    for (option in radioList) {
                        val inflater = LayoutInflater.from(context)
                        val button = inflater.inflate(
                            R.layout.radio_list_item_layout, null
                        ) as RadioButton
                        button.tag = option.key
                        button.setText(option.value)
                        radioGroup.addView(button)
                        if (option.key == currentValue) {
                            button.isChecked = true
                        }
                    }

                    actionButton.visibility =
                        if (actionButtonLabel.isNullOrEmpty()) View.GONE else View.VISIBLE

                }
            }
        })

        customSheetFragment.transparentBackground()
        customSheetFragment.launch()
    }

    class RadioListSheetHolder : PageHolder() {
        lateinit var actionButton: TextView
        lateinit var cancelButton: TextView
        lateinit var title: TextView
        lateinit var description: TextView
        lateinit var radioGroup: RadioGroup

        override fun bindView(view: View) {
            actionButton = view.findViewById(R.id.standard_sheet_confirm_btn)
            cancelButton = view.findViewById(R.id.standard_sheet_cancel_btn)
            title = view.findViewById(R.id.standard_sheet_title)
            description = view.findViewById(R.id.standard_sheet_content)
            radioGroup = view.findViewById(R.id.radio_list)
        }
    }

    interface RadioOptionConsumer {
        fun accept(option: Int)
    }

    @JvmStatic
    fun showRadioListOptionsSheet(
        fragmentManager: FragmentManager,
        context: Context,
        radioList: LinkedHashMap<Int, Int>,
        titleText: String?,
        descriptionText: String?,
        actionButtonLabel: String? = null,
        cancelButtonLabel: String? = null,
        consumer: RadioOptionConsumer
    ) {

        val customSheetFragment =
            CustomBottomSheetFragment.with(fragmentManager).page(R.layout.radio_list_sheet_layout)
                .cancellable(true).screenTag("RadioListOptionsSheet")

        customSheetFragment.holder(RadioListSheetHolder(), object : Binder<RadioListSheetHolder> {
            override fun onBind(holder: RadioListSheetHolder) {
                with(holder) {
                    title.text = titleText
                    description.text = descriptionText
                    actionButtonLabel?.let {
                        actionButton.text = it
                    }
                    cancelButtonLabel?.let {
                        cancelButton.text = it
                    }

                    actionButton.setOnClickListener {
                        val radioButton: AppCompatRadioButton =
                            radioGroup.findViewById(radioGroup.checkedRadioButtonId)
                        val option = radioButton.tag as Int
                        consumer.accept(option)
                        customSheetFragment.dismiss()
                    }

                    cancelButton.setOnClickListener {
                        customSheetFragment.dismiss()
                    }

                    for (option in radioList) {
                        val inflater = LayoutInflater.from(context)
                        val button = inflater.inflate(
                            R.layout.radio_list_item_layout, null
                        ) as RadioButton
                        button.tag = option.key
                        button.setText(option.value)
                        radioGroup.addView(button)
                    }

                    actionButton.visibility =
                        if (actionButtonLabel.isNullOrEmpty()) View.GONE else View.VISIBLE

                }
            }
        })

        customSheetFragment.transparentBackground()
        customSheetFragment.launch()
    }

    class DualChoiceSheetHolder : PageHolder() {
        lateinit var cancelButton: ImageView
        lateinit var buttonOne: RoundButton
        lateinit var buttonTwo: RoundButton
        lateinit var buttonThree: RoundButton
        lateinit var buttonFour: RoundButton
        lateinit var title: TextView
        lateinit var description: TextView
        lateinit var nextButton: TextView
        lateinit var backButton: TextView
        lateinit var descriptionContent: TextView
        lateinit var toolbarComponent: ToolbarComponent
        lateinit var unavailableConnexionText: TextView
        lateinit var unavailableConnexionTextDesc: TextView


        override fun bindView(view: View) {
            toolbarComponent = view.findViewById(R.id.toolbar)
            //  cancelButton = view.findViewById(R.id.standard_sheet_cancel_btn)
            buttonOne = view.findViewById(R.id.sheet_one_btn)
            buttonTwo = view.findViewById(R.id.sheet_two_btn)
            buttonThree = view.findViewById(R.id.sheet_three_btn)
            buttonFour = view.findViewById(R.id.sheet_four_btn)
            title = view.findViewById(R.id.standard_sheet_content)
            descriptionContent = view.findViewById(R.id.standard_sheet_content_description)
            nextButton = view.findViewById(R.id.next_btn)
            backButton = view.findViewById(R.id.back_btn)
            unavailableConnexionText = view.findViewById(R.id.unavailable_connection_text)
            unavailableConnexionTextDesc = view.findViewById(R.id.unavailable_connection_desc_text)
        }
    }

    interface BinaryConsumer {
        fun accept(option: Boolean)
    }

    interface IServerChoiceActions {
        fun addODKServer()
        fun addTellaWebServer()
        fun addUwaziServer()
        fun addGoogleDriveServer()
    }

    @JvmStatic
    fun showBinaryTypeSheet(
        fragmentManager: FragmentManager,
        titleText: String?,
        descriptionText: String?,
        descriptionContentText: String?,
        backText: String?,
        nextText: String?,
        buttonOneLabel: String? = null,
        buttonTwoLabel: String? = null,
        buttonThreeLabel: String? = null,
        buttonFourLabel: String? = null,
        unavailableConnexionLabel: String? = null,
        unavailableConnexionDesc: String? = null,
        isConnexionAvailable: Boolean = false,
        consumer: IServerChoiceActions
    ) {

        val customSheetFragment = CustomBottomSheetFragment.with(fragmentManager)
            .page(R.layout.settings_add_server_connecection_layout).cancellable(true).fullScreen()
            .statusBarColor(R.color.space_cadet)
        customSheetFragment.holder(DualChoiceSheetHolder(), object : Binder<DualChoiceSheetHolder> {
            @RequiresApi(Build.VERSION_CODES.M)
            override fun onBind(holder: DualChoiceSheetHolder) {
                with(holder) {
                    title.text = titleText
                    backButton.text = backText
                    nextButton.text = nextText
                    descriptionContent.text = descriptionContentText
                    buttonOne.setText(buttonOneLabel)
                    buttonTwo.setText(buttonTwoLabel)
                    buttonThree.setText(buttonThreeLabel)
                    buttonFour.setText(buttonFourLabel)
                    unavailableConnexionText.text = unavailableConnexionLabel;
                    unavailableConnexionTextDesc.text = unavailableConnexionDesc

                    if (isConnexionAvailable) {
                        unavailableConnexionText.isVisible = true
                        unavailableConnexionTextDesc.isVisible = true
                        buttonFour.setBackgroundColor(
                            unavailableConnexionText.context.getColor(
                                R.color.wa_white_8
                            )
                        )
                        buttonFour.setTextColor(R.color.wa_white_38)
                    } else {
                        unavailableConnexionText.isVisible = false
                        unavailableConnexionTextDesc.isVisible = false
                    }

                    buttonOne.setOnClickListener {
                        buttonOne.isChecked = true
                        buttonTwo.isChecked = false
                        buttonThree.isChecked = false
                        buttonFour.isChecked = false
                    }

                    buttonTwo.setOnClickListener {
                        buttonOne.isChecked = false
                        buttonTwo.isChecked = true
                        buttonThree.isChecked = false
                        buttonFour.isChecked = false
                    }

                    buttonThree.setOnClickListener {
                        buttonOne.isChecked = false
                        buttonTwo.isChecked = false
                        buttonFour.isChecked = false
                        buttonThree.isChecked = true
                    }

                    buttonFour.setOnClickListener {
                        buttonOne.isChecked = false
                        buttonTwo.isChecked = false
                        buttonThree.isChecked = false
                        buttonFour.isChecked = true
                    }

                    backButton.setOnClickListener {
                        customSheetFragment.dismiss()
                    }

                    nextButton.setOnClickListener {
                        when {
                            buttonOne.isChecked -> {
                                consumer.addODKServer()
                                customSheetFragment.dismiss()
                            }

                            buttonTwo.isChecked -> {
                                consumer.addTellaWebServer()
                                customSheetFragment.dismiss()
                            }

                            buttonFour.isChecked -> {
                                consumer.addGoogleDriveServer()
                                customSheetFragment.dismiss()
                            }

                            else -> {
                                consumer.addUwaziServer()
                                customSheetFragment.dismiss()
                            }
                        }
                    }

                }

            }
        })

        customSheetFragment.transparentBackground()
        customSheetFragment.launch()
    }

    class CamouflageSheetHolder : PageHolder() {
        lateinit var sheetTitle: TextView
        lateinit var sheetsubTitle: TextView
        lateinit var cancelButton: ImageView
        lateinit var buttonOneTitle: TextView
        lateinit var buttonOneSubtitle: TextView
        lateinit var title: TextView
        lateinit var buttonTwoTitle: TextView
        lateinit var buttonTwoSubtitle: TextView
        lateinit var buttonOne: View
        lateinit var buttonTwo: View

        override fun bindView(view: View) {
            title = view.findViewById(R.id.dialog_title)
            cancelButton = view.findViewById(R.id.standard_sheet_cancel_btn)
            buttonOneTitle = view.findViewById(R.id.title_btn_one)
            buttonOneSubtitle = view.findViewById(R.id.subtitle_btn_one)
            sheetTitle = view.findViewById(R.id.sheet_title)
            sheetsubTitle = view.findViewById(R.id.sheet_subtitle)
            buttonOne = view.findViewById(R.id.sheet_one_btn)
            buttonTwo = view.findViewById(R.id.sheet_two_btn)
            buttonTwoTitle = view.findViewById(R.id.title_btn_two)
            buttonTwoSubtitle = view.findViewById(R.id.subtitle_btn_two)
        }
    }


    @JvmStatic
    fun showChangeCamouflageSheet(
        fragmentManager: FragmentManager,
        titleText: String?,
        dialogTitle: String?,
        dialogSubtitle: String?,
        titleOne: String?,
        subtitleOne: String?,
        titleTwo: String?,
        subtitleTwo: String?,
        consumer: BinaryConsumer? = null
    ) {

        val customSheetFragment =
            CustomBottomSheetFragment.with(fragmentManager).page(R.layout.change_camouflage_layout)
                .cancellable(true).fullScreen().statusBarColor(R.color.space_cadet)
        customSheetFragment.holder(CamouflageSheetHolder(), object : Binder<CamouflageSheetHolder> {
            override fun onBind(holder: CamouflageSheetHolder) {
                with(holder) {
                    title.text = titleText
                    buttonOneTitle.text = titleOne
                    buttonOneSubtitle.text = subtitleOne
                    sheetTitle.text = dialogTitle
                    sheetsubTitle.text = dialogSubtitle
                    buttonTwoTitle.text = titleTwo
                    buttonTwoSubtitle.text = subtitleTwo

                    buttonOne.setOnClickListener {
                        consumer?.accept(true)
                        customSheetFragment.dismiss()
                    }

                    buttonTwo.setOnClickListener {
                        consumer?.accept(false)
                        customSheetFragment.dismiss()
                    }

                    cancelButton.setOnClickListener {
                        customSheetFragment.dismiss()
                    }
                }
            }
        })

        customSheetFragment.transparentBackground()
        customSheetFragment.launch()
    }

    interface ActionConfirmed {
        fun accept(isConfirmed: Boolean)
    }

    @JvmStatic
    fun showConfirmSheet(
        fragmentManager: FragmentManager,
        titleText: String?,
        descriptionText: String?,
        actionButtonLabel: String? = null,
        cancelButtonLabel: String? = null,
        consumer: ActionConfirmed
    ) {

        val customSheetFragment =
            CustomBottomSheetFragment.with(fragmentManager).page(R.layout.standar_sheet_layout)
                .cancellable(true).screenTag("ConfirmSheet")
        customSheetFragment.holder(GenericSheetHolder(), object : Binder<GenericSheetHolder> {
            override fun onBind(holder: GenericSheetHolder) {
                with(holder) {
                    title.text = titleText
                    description.text = descriptionText
                    actionButtonLabel?.let {
                        actionButton.text = it
                    }
                    cancelButtonLabel?.let {
                        cancelButton.text = it
                    }

                    actionButton.setOnClickListener {
                        consumer.accept(isConfirmed = true)
                        customSheetFragment.dismiss()
                    }

                    cancelButton.setOnClickListener {
                        consumer.accept(isConfirmed = false)
                        customSheetFragment.dismiss()
                    }

                    actionButton.visibility =
                        if (actionButtonLabel.isNullOrEmpty()) View.GONE else View.VISIBLE

                }
            }
        })

        customSheetFragment.transparentBackground()
        customSheetFragment.launch()
    }


    @JvmStatic
    fun showChooseImportSheet(
        fragmentManager: FragmentManager,
        titleText: String?,
        descriptionText: String?,
        importButtonLabel: String? = null,
        importAndDeleteButtonLabel: String? = null,
        importConsumer: ActionConfirmed,
        importAndDeleteConsumer: ActionConfirmed

    ) {

        val customSheetFragment =
            CustomBottomSheetFragment.with(fragmentManager).page(R.layout.standar_sheet_layout)
                .cancellable(true).screenTag("ConfirmSheet")
        customSheetFragment.holder(GenericSheetHolder(), object : Binder<GenericSheetHolder> {
            override fun onBind(holder: GenericSheetHolder) {
                with(holder) {
                    title.text = titleText
                    description.text = descriptionText
                    importButtonLabel?.let {
                        actionButton.text = it
                    }
                    importAndDeleteButtonLabel?.let {
                        cancelButton.text = it
                    }

                    actionButton.setOnClickListener {
                        importAndDeleteConsumer.accept(isConfirmed = true)
                        customSheetFragment.dismiss()
                    }

                    cancelButton.setOnClickListener {
                        importConsumer.accept(isConfirmed = true)
                        customSheetFragment.dismiss()
                    }


                }
            }
        })

        customSheetFragment.transparentBackground()
        customSheetFragment.launch()
    }

    fun showProgressImportSheet(
        fragmentManager: FragmentManager,
        titleText: String?,
        totalProgress: Int,
        progressNumberText: String,
        progressStatus: MutableLiveData<Int>,
        cancelText: String,
        lifecycleOwner: LifecycleOwner,
        onCancelImport: (() -> Unit)

    ) {
        val customSheetFragment =
            CustomBottomSheetFragment.with(fragmentManager).page(R.layout.layout_progess_sheet)
                .cancellable(true).statusBarColor(R.color.space_cadet)
        customSheetFragment.holder(DownloadStatustHolder(), object : Binder<DownloadStatustHolder> {
            override fun onBind(holder: DownloadStatustHolder) {
                with(holder) {
                    progressStatus.observe(lifecycleOwner) { status ->
                        subtitle.text = "$status/$totalProgress $progressNumberText"
                        val statusPercent = status * 100 / totalProgress
                        circularProgress.setProgressPercent(statusPercent, true)
                        linearProgress.setProgressPercent(statusPercent, true)
                        if ((status == totalProgress) && (customSheetFragment.isAdded)) {
                            customSheetFragment.dismiss()
                        }
                    }
                    title.text = titleText
                    cancelTextView.text = cancelText
                    cancelTextView.setOnClickListener {
                        onCancelImport.invoke()
                        customSheetFragment.dismiss()
                    }

                }
            }
        })

        customSheetFragment.transparentBackground()
        customSheetFragment.launch()
    }


    class ConfirmImageSheetHolder : PageHolder() {
        lateinit var actionButton: TextView
        lateinit var cancelButton: TextView
        lateinit var title: TextView
        lateinit var description: TextView
        lateinit var imageView: ImageView

        override fun bindView(view: View) {
            actionButton = view.findViewById(R.id.standard_sheet_confirm_btn)
            cancelButton = view.findViewById(R.id.standard_sheet_cancel_btn)
            title = view.findViewById(R.id.standard_sheet_title)
            description = view.findViewById(R.id.standard_sheet_content)
            imageView = view.findViewById(R.id.sheet_image)
        }
    }

    @JvmStatic
    fun showConfirmSheetWithImageAndTimeout(
        fragmentManager: FragmentManager,
        timeoutTitleText: String?,
        timeoutTitleDesc: String?,
        titleText: String?,
        descriptionText: String?,
        actionButtonLabel: String? = null,
        cancelButtonLabel: String? = null,
        confirmDrawable: Drawable? = null,
        consumer: ActionConfirmed
    ) {

        val customSheetFragment2 = CustomBottomSheetFragment.with(fragmentManager)
            .page(R.layout.confirm_image_sheet_layout).cancellable(false)
        customSheetFragment2.holder(
            ConfirmImageSheetHolder(),
            object : Binder<ConfirmImageSheetHolder> {
                override fun onBind(holder: ConfirmImageSheetHolder) {
                    with(holder) {
                        title.text = timeoutTitleText
                        description.text = timeoutTitleDesc
                        confirmDrawable?.let {
                            imageView.setImageDrawable(it)
                        }
                        actionButton.visibility = View.GONE
                        cancelButton.visibility = View.GONE

                        Handler().postDelayed({
                            consumer.accept(isConfirmed = true)
                            //customSheetFragment2.dismiss()
                        }, LONG_TIMEOUT)

                    }
                }
            })

        val customSheetFragment = CustomBottomSheetFragment.with(fragmentManager)
            .page(R.layout.confirm_image_sheet_layout).cancellable(true)
            .screenTag("ConfirmImageSheet")
        customSheetFragment.holder(
            ConfirmImageSheetHolder(),
            object : Binder<ConfirmImageSheetHolder> {
                override fun onBind(holder: ConfirmImageSheetHolder) {
                    with(holder) {
                        title.text = titleText
                        description.text = descriptionText
                        actionButtonLabel?.let {
                            actionButton.text = it
                        }
                        cancelButtonLabel?.let {
                            cancelButton.text = it
                        }

                        actionButton.setOnClickListener {
                            fragmentManager.beginTransaction()
                                .add(customSheetFragment2, customSheetFragment2.tag).commit()
                            customSheetFragment.dismiss()
                        }

                        cancelButton.setOnClickListener {
                            customSheetFragment.dismiss()
                        }

                        confirmDrawable?.let {
                            imageView.setImageDrawable(it)
                        }

                        actionButton.visibility =
                            if (actionButtonLabel.isNullOrEmpty()) View.GONE else View.VISIBLE

                    }
                }
            })

        customSheetFragment.transparentBackground()
        customSheetFragment.launch()
    }

    interface UploadServerConsumer {
        fun accept(serverId: Long)
    }

    @JvmStatic
    fun showWarningSheetWithImageAndTimeout(
        fragmentManager: FragmentManager,
        timeoutTitleText: String?,
        timeoutTitleDesc: String?,
        confirmDrawable: Drawable? = null,
        consumer: ActionConfirmed,
        timeout: Long
    ) {

        val customSheetFragment = CustomBottomSheetFragment.with(fragmentManager)
            .page(R.layout.confirm_image_sheet_layout).cancellable(false)
        customSheetFragment.holder(
            ConfirmImageSheetHolder(),
            object : Binder<ConfirmImageSheetHolder> {
                override fun onBind(holder: ConfirmImageSheetHolder) {
                    with(holder) {
                        title.text = timeoutTitleText
                        description.text = timeoutTitleDesc
                        confirmDrawable?.let {
                            imageView.setImageDrawable(it)
                        }
                        actionButton.visibility = View.GONE
                        cancelButton.visibility = View.GONE

                        Handler().postDelayed({
                            consumer.accept(isConfirmed = true)
                            customSheetFragment.dismiss()
                        }, timeout)

                    }
                }
            })


        customSheetFragment.transparentBackground()
        customSheetFragment.launch()
    }

    @JvmStatic
    fun showChooseAutoUploadServerSheet(
        fragmentManager: FragmentManager,
        titleText: String?,
        descriptionText: String?,
        actionButtonLabel: String? = null,
        cancelButtonLabel: String? = null,
        radioList: LinkedHashMap<Long, String>,
        currentServerId: Long,
        context: Context,
        consumer: UploadServerConsumer
    ) {

        val customSheetFragment =
            CustomBottomSheetFragment.with(fragmentManager).page(R.layout.radio_list_sheet_layout)
                .cancellable(true)
        customSheetFragment.holder(RadioListSheetHolder(), object : Binder<RadioListSheetHolder> {
            override fun onBind(holder: RadioListSheetHolder) {
                with(holder) {
                    title.text = titleText
                    description.text = descriptionText
                    actionButtonLabel?.let {
                        actionButton.text = it
                    }
                    cancelButtonLabel?.let {
                        cancelButton.text = it
                    }

                    actionButton.setOnClickListener {
                        val radioButton: AppCompatRadioButton =
                            radioGroup.findViewById(radioGroup.checkedRadioButtonId)
                        val option = radioButton.tag as Long
                        consumer.accept(option)
                        customSheetFragment.dismiss()
                    }

                    cancelButton.setOnClickListener {
                        customSheetFragment.dismiss()
                    }

                    for (option in radioList) {
                        val inflater = LayoutInflater.from(context)
                        val button = inflater.inflate(
                            R.layout.radio_list_item_layout, null
                        ) as RadioButton
                        button.tag = option.key
                        button.text = option.value
                        radioGroup.addView(button)
                        if (option.key == currentServerId) {
                            button.isChecked = true
                        }
                    }

                    actionButton.visibility =
                        if (actionButtonLabel.isNullOrEmpty()) View.GONE else View.VISIBLE

                }
            }
        })

        customSheetFragment.transparentBackground()
        customSheetFragment.launch()
    }

    enum class Action {
        EDIT, DELETE, SHARE, VIEW
    }

    class ServerMenuSheetHolder : PageHolder() {
        lateinit var actionEdit: TextView
        lateinit var actionDelete: TextView
        lateinit var title: TextView

        override fun bindView(view: View) {
            actionEdit = view.findViewById(R.id.action_edit)
            actionDelete = view.findViewById(R.id.action_delete)
            title = view.findViewById(R.id.standard_sheet_title)
        }
    }

    class ThreeOptionsMenuSheetHolder : PageHolder() {
        lateinit var actionView: TextView
        lateinit var actionShare: TextView
        lateinit var actionDelete: TextView
        lateinit var title: TextView

        override fun bindView(view: View) {
            actionView = view.findViewById(R.id.action_view)
            actionShare = view.findViewById(R.id.action_share)
            actionDelete = view.findViewById(R.id.action_delete)
            title = view.findViewById(R.id.standard_sheet_title)
        }
    }

    interface ActionSeleceted {
        fun accept(action: Action)
    }

    @JvmStatic
    fun showEditDeleteMenuSheet(
        fragmentManager: FragmentManager,
        titleText: String?,
        actionEditLabel: String? = null,
        actionDeleteLabel: String? = null,
        consumer: ActionSeleceted,
        titleText2: String?,
        descriptionText: String?,
        actionButtonLabel: String? = null,
        cancelButtonLabel: String? = null,
        iconView: Int = -1
    ) {

        val customSheetFragment2 =
            CustomBottomSheetFragment.with(fragmentManager).page(R.layout.standar_sheet_layout)
                .cancellable(true)
        customSheetFragment2.holder(GenericSheetHolder(), object : Binder<GenericSheetHolder> {
            override fun onBind(holder: GenericSheetHolder) {
                with(holder) {
                    title.text = titleText2
                    description.text = descriptionText
                    actionButtonLabel?.let {
                        actionButton.text = it
                    }
                    cancelButtonLabel?.let {
                        cancelButton.text = it
                    }

                    actionButton.setOnClickListener {
                        consumer.accept(action = Action.DELETE)
                        customSheetFragment2.dismiss()
                    }

                    cancelButton.setOnClickListener {
                        customSheetFragment2.dismiss()
                    }

                    actionButton.visibility =
                        if (actionButtonLabel.isNullOrEmpty()) View.GONE else View.VISIBLE

                }
            }
        })

        val customSheetFragment =
            CustomBottomSheetFragment.with(fragmentManager).page(R.layout.server_menu_sheet_layout)
                .cancellable(true)
        customSheetFragment.holder(ServerMenuSheetHolder(), object : Binder<ServerMenuSheetHolder> {
            override fun onBind(holder: ServerMenuSheetHolder) {
                with(holder) {
                    title.text = titleText

                    if (iconView != -1) {
                        actionEdit.setCompoundDrawablesWithIntrinsicBounds(iconView, 0, 0, 0)
                    }
                    actionEditLabel?.let {
                        actionEdit.text = it
                    }
                    actionDeleteLabel?.let {
                        actionDelete.text = it
                    }

                    actionEdit.setOnClickListener {
                        consumer.accept(action = Action.EDIT)
                        customSheetFragment.dismiss()
                    }

                    actionDelete.setOnClickListener {
                        //consumer.accept(action = Action.DELETE)
                        fragmentManager.beginTransaction()
                            .add(customSheetFragment2, customSheetFragment2.tag).commit()
                        customSheetFragment.dismiss()
                    }
                }
            }
        })

        customSheetFragment.transparentBackground()
        customSheetFragment.launch()
    }

    class RenameFileSheetHolder : PageHolder() {
        lateinit var actionCancel: TextView
        lateinit var actionRename: TextView
        lateinit var title: TextView
        lateinit var renameEditText: EditText

        override fun bindView(view: View) {
            actionRename = view.findViewById(R.id.standard_sheet_confirm_btn)
            actionCancel = view.findViewById(R.id.standard_sheet_cancel_btn)
            title = view.findViewById(R.id.standard_sheet_title)
            renameEditText = view.findViewById(R.id.renameEditText)
        }
    }

    @JvmStatic
    fun showFileRenameSheet(
        fragmentManager: FragmentManager,
        titleText: String?,
        cancelLabel: String,
        confirmLabel: String,
        context: Activity,
        fileName: String?,
        onConfirmClick: ((String) -> Unit)? = null
    ) {
        val renameFileSheet =
            CustomBottomSheetFragment.with(fragmentManager).page(R.layout.sheet_rename)
                .screenTag("FileRenameSheet").cancellable(true)
        renameFileSheet.holder(RenameFileSheetHolder(), object : Binder<RenameFileSheetHolder> {
            override fun onBind(holder: RenameFileSheetHolder) {
                with(holder) {
                    title.text = titleText
                    renameEditText.setText(fileName)
                    //Cancel action
                    actionCancel.text = cancelLabel
                    actionCancel.setOnClickListener { renameFileSheet.dismiss() }

                    //Rename action
                    actionRename.text = confirmLabel
                    actionRename.setOnClickListener {
                        if (!renameEditText.text.isNullOrEmpty()) {
                            renameFileSheet.dismiss()
                            onConfirmClick?.invoke(renameEditText.text.toString())
                        } else {
                            DialogUtils.showBottomMessage(
                                context, "Please fill in the new name", true
                            )
                        }

                    }
                }
            }
        })
        renameFileSheet.transparentBackground()
        renameFileSheet.launch()
    }

    class EnterCodeSheetHolder : PageHolder() {
        lateinit var title: TextView
        lateinit var subtitle: TextView
        lateinit var description: TextView
        lateinit var enterText: EditText
        lateinit var buttonNext: TextView
        lateinit var cancelButton: ImageView


        override fun bindView(view: View) {
            title = view.findViewById(R.id.sheet_title)
            subtitle = view.findViewById(R.id.sheet_subtitle)
            description = view.findViewById(R.id.sheet_description)
            enterText = view.findViewById(R.id.code_editText)
            buttonNext = view.findViewById(R.id.next_btn)
            cancelButton = view.findViewById(R.id.standard_sheet_cancel_btn)
        }
    }

    @JvmStatic
    fun showEnterCustomizationCodeSheet(
        fragmentManager: FragmentManager,
        titleText: String?,
        subTitle: String?,
        descriptionText: String?,
        nextButton: String?,
        consumer: StringConsumer? = null
    ) {

        val customSheetFragment = CustomBottomSheetFragment.with(fragmentManager)
            .page(R.layout.enter_string_bottomsheet_layout).cancellable(true).fullScreen()
            .statusBarColor(R.color.space_cadet)
        customSheetFragment.holder(EnterCodeSheetHolder(), object : Binder<EnterCodeSheetHolder> {
            override fun onBind(holder: EnterCodeSheetHolder) {
                with(holder) {
                    title.text = titleText
                    subtitle.text = subTitle
                    description.text = descriptionText
                    buttonNext.text = nextButton
                    buttonNext.setOnClickListener {
                        if (enterText.text.isNotEmpty()) {
                            consumer?.accept(enterText.text.toString())
                            customSheetFragment.dismiss()
                        }
                    }
                    cancelButton.setOnClickListener {
                        customSheetFragment.dismiss()
                    }
                }
            }
        })

        customSheetFragment.transparentBackground()
        customSheetFragment.launch()
    }

    interface StringConsumer {
        fun accept(code: String)
    }

    @JvmStatic
    fun showDownloadStatus(
        fragmentManager: FragmentManager,
        titleText: String?,
        completeText: String,
        progressStatus: AtomicInteger,
        cancelText: String,
        onCancelClick: (() -> Unit)?
    ) {
        val customSheetFragment =
            CustomBottomSheetFragment.with(fragmentManager).page(R.layout.layout_progess_sheet)
                .cancellable(true).fullScreen().statusBarColor(R.color.space_cadet)
        customSheetFragment.holder(DownloadStatustHolder(), object : Binder<DownloadStatustHolder> {
            override fun onBind(holder: DownloadStatustHolder) {
                with(holder) {
                    title.text = titleText
                    subtitle.text = "$progressStatus% $completeText"
                    cancelTextView.text = cancelText
                    circularProgress.progress = progressStatus.get()
                    linearProgress.progress = progressStatus.get()
                    if (progressStatus.get() == 100) {
                        customSheetFragment.dismiss()
                    }
                    cancelTextView.setOnClickListener {
                        onCancelClick?.invoke()
                        customSheetFragment.dismiss()
                    }
                }
            }
        })

        customSheetFragment.transparentBackground()
        customSheetFragment.launch()
    }

    class DownloadStatustHolder : PageHolder() {
        lateinit var title: TextView
        lateinit var subtitle: TextView
        lateinit var cancelTextView: TextView
        lateinit var circularProgress: CircularProgressIndicator
        lateinit var linearProgress: LinearProgressIndicator


        override fun bindView(view: View) {
            title = view.findViewById(R.id.standard_sheet_title)
            subtitle = view.findViewById(R.id.tv_progress_indicator)
            cancelTextView = view.findViewById(R.id.tv_cancel)
            circularProgress = view.findViewById(R.id.progress_circular)
            linearProgress = view.findViewById(R.id.progress_linear)
        }
    }

    @JvmStatic
    fun showConfirmDelete(
        fragmentManager: FragmentManager,
        titleText: String,
        confirm: String,
        onConfirmClick: (() -> Unit)?
    ) {
        val customSheetFragment =
            CustomBottomSheetFragment.with(fragmentManager).page(R.layout.sheet_confirm_delete)
                .cancellable(true).statusBarColor(R.color.space_cadet)
        customSheetFragment.holder(ConfirmDeletetHolder(), object : Binder<ConfirmDeletetHolder> {
            override fun onBind(holder: ConfirmDeletetHolder) {
                with(holder) {
                    title.text = titleText
                    confirmTextView.text = confirm

                    confirmTextView.setOnClickListener {
                        onConfirmClick?.invoke()
                        customSheetFragment.dismiss()
                    }
                }
            }
        })

        customSheetFragment.transparentBackground()
        customSheetFragment.launch()
    }

    class ConfirmDeletetHolder : PageHolder() {
        lateinit var title: TextView
        lateinit var confirmTextView: TextView


        override fun bindView(view: View) {
            title = view.findViewById(R.id.standard_sheet_title)
            confirmTextView = view.findViewById(R.id.sheet_cancel)
        }
    }

    @JvmStatic
    fun showThreeOptionMenuSheet(
        fragmentManager: FragmentManager,
        titleText: String?,
        actionViewLabel: String? = null,
        actionShareLabel: String? = null,
        actionDeleteLabel: String? = null,
        consumer: ActionSeleceted,
        titleText2: String?,
        descriptionText: String?,
        actionButtonLabel: String? = null,
        cancelButtonLabel: String? = null
    ) {

        val customSheetFragment2 =
            CustomBottomSheetFragment.with(fragmentManager).page(R.layout.standar_sheet_layout)
                .cancellable(true)
        customSheetFragment2.holder(GenericSheetHolder(), object : Binder<GenericSheetHolder> {
            override fun onBind(holder: GenericSheetHolder) {
                with(holder) {
                    title.text = titleText2
                    description.text = descriptionText
                    actionButtonLabel?.let {
                        actionButton.text = it
                    }
                    cancelButtonLabel?.let {
                        cancelButton.text = it
                    }

                    actionButton.setOnClickListener {
                        consumer.accept(action = Action.DELETE)
                        customSheetFragment2.dismiss()
                    }

                    cancelButton.setOnClickListener {
                        customSheetFragment2.dismiss()
                    }

                    actionButton.visibility =
                        if (actionButtonLabel.isNullOrEmpty()) View.GONE else View.VISIBLE

                }
            }
        })

        val customSheetFragment = CustomBottomSheetFragment.with(fragmentManager)
            .page(R.layout.three_options_sheet_layout).cancellable(true)
        customSheetFragment.holder(
            ThreeOptionsMenuSheetHolder(),
            object : Binder<ThreeOptionsMenuSheetHolder> {
                override fun onBind(holder: ThreeOptionsMenuSheetHolder) {
                    with(holder) {
                        title.text = titleText
                        actionViewLabel?.let {
                            actionView.text = it
                        }

                        if (actionShareLabel == null) {
                            actionShare.visibility = View.GONE
                        } else {
                            actionShare.text = actionShareLabel
                        }

                        actionDeleteLabel?.let {
                            actionDelete.text = it
                        }

                        actionView.setOnClickListener {
                            consumer.accept(action = Action.VIEW)
                            customSheetFragment.dismiss()
                        }

                        actionShare.setOnClickListener {
                            consumer.accept(action = Action.SHARE)
                            customSheetFragment.dismiss()
                        }

                        actionDelete.setOnClickListener {
                            //consumer.accept(action = Action.DELETE)
                            fragmentManager.beginTransaction()
                                .add(customSheetFragment2, customSheetFragment2.tag).commit()
                            customSheetFragment.dismiss()
                        }
                    }
                }
            })

        customSheetFragment.transparentBackground()
        customSheetFragment.launch()
    }

    @JvmStatic
    fun showViewDeleteMenuSheet(
        fragmentManager: FragmentManager,
        titleText: String?,
        actionEditLabel: String? = null,
        actionDeleteLabel: String? = null,
        consumer: ActionSeleceted,
        titleText2: String?,
        descriptionText: String?,
        actionButtonLabel: String? = null,
        cancelButtonLabel: String? = null,
        iconView: Int = -1
    ) {

        val customSheetFragment2 =
            CustomBottomSheetFragment.with(fragmentManager).page(R.layout.standar_sheet_layout)
                .cancellable(true)
        customSheetFragment2.holder(GenericSheetHolder(), object : Binder<GenericSheetHolder> {
            override fun onBind(holder: GenericSheetHolder) {
                with(holder) {
                    title.text = titleText2
                    description.text = descriptionText
                    actionButtonLabel?.let {
                        actionButton.text = it
                    }
                    cancelButtonLabel?.let {
                        cancelButton.text = it
                    }

                    actionButton.setOnClickListener {
                        consumer.accept(action = Action.DELETE)
                        customSheetFragment2.dismiss()
                    }

                    cancelButton.setOnClickListener {
                        customSheetFragment2.dismiss()
                    }

                    actionButton.visibility =
                        if (actionButtonLabel.isNullOrEmpty()) View.GONE else View.VISIBLE

                }
            }
        })

        val customSheetFragment = CustomBottomSheetFragment.with(fragmentManager)
            .page(R.layout.view_delete_menu_sheet_layout).cancellable(true)
        customSheetFragment.holder(ServerMenuSheetHolder(), object : Binder<ServerMenuSheetHolder> {
            override fun onBind(holder: ServerMenuSheetHolder) {
                with(holder) {
                    title.text = titleText

                    if (iconView != -1) {
                        actionEdit.setCompoundDrawablesWithIntrinsicBounds(iconView, 0, 0, 0)
                    }
                    actionEditLabel?.let {
                        actionEdit.text = it
                    }
                    actionDeleteLabel?.let {
                        actionDelete.text = it
                    }

                    actionEdit.setOnClickListener {
                        consumer.accept(action = Action.VIEW)
                        customSheetFragment.dismiss()
                    }

                    actionDelete.setOnClickListener {
                        //consumer.accept(action = Action.DELETE)
                        fragmentManager.beginTransaction()
                            .add(customSheetFragment2, customSheetFragment2.tag).commit()
                        customSheetFragment.dismiss()
                    }
                }
            }
        })

        customSheetFragment.transparentBackground()
        customSheetFragment.launch()
    }
}


