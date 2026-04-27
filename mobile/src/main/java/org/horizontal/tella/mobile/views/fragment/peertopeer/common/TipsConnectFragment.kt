package org.horizontal.tella.mobile.views.fragment.peertopeer.common

import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.text.style.LeadingMarginSpan
import android.view.View
import androidx.activity.addCallback
import androidx.core.content.ContextCompat
import org.horizontal.tella.mobile.R
import org.horizontal.tella.mobile.util.Util
import org.horizontal.tella.mobile.databinding.FragmentTipsConnectBinding
import org.horizontal.tella.mobile.views.base_ui.BaseBindingFragment

class TipsConnectFragment :
    BaseBindingFragment<FragmentTipsConnectBinding>(FragmentTipsConnectBinding::inflate) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        binding.toolbar.backClickListener =
            { requireActivity().onBackPressedDispatcher.onBackPressed() }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            isEnabled = false
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        binding.section1List.text = buildNumberedConnectText()
        binding.section3Bullets.text = buildMoreTipsText()
        binding.section3Bullets.movementMethod = LinkMovementMethod.getInstance()
    }

    private fun buildNumberedConnectText(): CharSequence {
        val builder = SpannableStringBuilder()
        builder.append(getString(R.string.nearbysharing_tips_to_connect_intro))
        builder.append("\n")
        appendNumberedItem(builder, 1, getString(R.string.nearbysharing_tips_to_connect_item_1))
        builder.append("\n")
        appendNumberedItem(builder, 2, getString(R.string.nearbysharing_tips_to_connect_item_2))
        return builder
    }

    private fun appendNumberedItem(builder: SpannableStringBuilder, number: Int, text: String) {
        val start = builder.length
        builder.append(" $number. ")
        builder.append(text)
        builder.setSpan(
            LeadingMarginSpan.Standard(0, dpToPx(20)),
            start,
            builder.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }

    private fun buildMoreTipsText(): CharSequence {
        val builder = SpannableStringBuilder()
        appendBulletedItem(builder, getString(R.string.tips_more_item_1))
        builder.append("\n")
        appendBulletedItem(builder, getString(R.string.tips_more_item_2))

        val fullText = builder.toString()
        val clickableWord = "documentation"
        val start = fullText.indexOf(clickableWord)
        if (start >= 0) {
            val end = start + clickableWord.length
            builder.setSpan(
                object : ClickableSpan() {
                    override fun onClick(widget: View) {
                        Util.startBrowserIntent(context, getString(R.string.peerToPeer_documentation_url))
                    }

                    override fun updateDrawState(ds: TextPaint) {
                        ds.isUnderlineText = false
                    }
                },
                start,
                end,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            builder.setSpan(
                ForegroundColorSpan(
                    ContextCompat.getColor(requireContext(), R.color.wa_yellow)
                ),
                start,
                end,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        return builder
    }

    private fun appendBulletedItem(builder: SpannableStringBuilder, text: String) {
        val start = builder.length
        builder.append(" • ")
        builder.append(text)
        builder.setSpan(
            LeadingMarginSpan.Standard(0, dpToPx(20)),
            start,
            builder.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }
}