package com.jakting.shareclean.activity

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.lifecycle.lifecycleScope
import com.drake.brv.utils.BRV
import com.drake.brv.utils.setup
import com.google.android.material.card.MaterialCardView
import com.jakting.shareclean.BR
import com.jakting.shareclean.BaseActivity
import com.jakting.shareclean.R
import com.jakting.shareclean.data.App
import com.jakting.shareclean.data.AppIntent
import com.jakting.shareclean.databinding.ActivityDetailsBinding
import com.jakting.shareclean.utils.MyApplication.Companion.intentIconMap
import com.jakting.shareclean.utils.getAppDetail
import com.jakting.shareclean.utils.getAppIconByPackageName
import kotlinx.coroutines.launch

class DetailsActivity : BaseActivity() {

    private lateinit var binding: ActivityDetailsBinding
    private var app = App()

    override fun onCreate(savedInstanceState: Bundle?) {
        binding = ActivityDetailsBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        app = intent.extras?.get("app") as App
        initView()
    }

    private fun initView() {
        binding.appName.text = app.appName
        binding.appPackageName.text = app.packageName
        binding.appVersionName.text =
            String.format(
                getString(R.string.manager_clean_detail_version),
                getAppDetail(app.packageName).versionName,
                getAppDetail(app.packageName).versionCode
            )
        lifecycleScope.launch {
            binding.appIcon.setImageDrawable(
                getAppIconByPackageName(
                    this@DetailsActivity,
                    app.packageName
                )
            )
        }
        binding.cardSystemAppWarning.visibility = if (app.isSystem) View.VISIBLE else View.GONE
        BRV.modelId = BR.app
        binding.rv.setup {
            addType<AppIntent>(R.layout.item_intent)
            onBind {
                val appIcon = findView<ImageView>(R.id.app_component_icon)
                lifecycleScope.launch {
                    val keyIcon = getModel<AppIntent>().packageName+"/"+getModel<AppIntent>().component
                    appIcon.setImageDrawable(intentIconMap[keyIcon])
                }
                val cardView = findView<MaterialCardView>(R.id.app_card)
                cardView.isChecked = getModel<AppIntent>().checked
            }
            onChecked { position, isChecked, isAllChecked ->
                val model = getModel<AppIntent>(position)
                model.checked = isChecked
                model.notifyChange() // 通知UI跟随数据变化
            }

            R.id.app_layout.onClick(){
                val checked = (getModel() as AppIntent).checked
                setChecked(adapterPosition, checked) // 在点击事件中触发选择事件, 即点击列表条目就选中

            }
        }.models = app.intentList
    }


}