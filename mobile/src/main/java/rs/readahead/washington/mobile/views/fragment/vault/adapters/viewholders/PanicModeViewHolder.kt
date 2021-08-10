package rs.readahead.washington.mobile.views.fragment.vault.adapters.viewholders

import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import rs.readahead.washington.mobile.R
import rs.readahead.washington.mobile.views.fragment.vault.adapters.VaultClickListener
import rs.readahead.washington.mobile.views.fragment.vault.adapters.viewholders.base.BaseViewHolder
import rs.readahead.washington.mobile.views.fragment.vault.adapters.viewholders.base.inflate
import rs.readahead.washington.mobile.views.fragment.vault.adapters.viewholders.data.VaultFile

class PanicModeViewHolder (val view : View) : BaseViewHolder<VaultFile>(view) {
    private lateinit var panicSeekBar : SeekBar

    override fun bind(item: VaultFile, vaultClickListener: VaultClickListener) {
        panicSeekBar = view.findViewById(R.id.panic_seek)

        panicSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, i: Int, b: Boolean) {
                if (b) {
                    blendPanicScreens(i)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
                if (seekBar.progress > 0) {
                }
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                if (seekBar.progress == 100) {
                    vaultClickListener.onPanicModeSwipeListener(progress = seekBar.progress)
                    seekBar.progress = 0
                } else {

                }
            }
        })

    }

    companion object {
        fun from(parent: ViewGroup): PanicModeViewHolder {
            return PanicModeViewHolder(parent.inflate(R.layout.item_vault_panic_button))
        }
    }

    private fun blendPanicScreens(i: Int) {
        panicSeekBar.visibility = if (i == 100) View.INVISIBLE else View.VISIBLE
    }

}