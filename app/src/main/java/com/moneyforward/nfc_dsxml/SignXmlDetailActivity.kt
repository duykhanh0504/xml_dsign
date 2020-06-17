package com.moneyforward.nfc_dsxml

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_sign_xml_detail.*
import com.google.android.material.tabs.TabLayout


/**
 * Created by Kan on 2020-06-17
 * Copyright © 2018 Money Forward, Inc. All rights
 */

class SignXmlDetailActivity : AppCompatActivity() {

    companion object {
        private const val ORIGINAL_XML = "original_xml"
        private const val SIGNATURE_XML = "signature_xml"

        private const val ORIGINAL_INDEX = 1
        private const val SIGNATURE_INDEX = 0

        fun createIntent(context: Context, original: String, signature: String) =
            Intent(context, SignXmlDetailActivity::class.java).apply {
                putExtra(ORIGINAL_XML, original)
                putExtra(SIGNATURE_XML, signature)
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_xml_detail)
        tabLayout.addTab(tabLayout.newTab().setText("Signature"))
        tabLayout.addTab(tabLayout.newTab().setText("Original"))
        tvDetail.text = intent.getStringExtra(SIGNATURE_XML)

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                if (tab.position == SIGNATURE_INDEX) {
                    tvDetail.text = intent.getStringExtra(SIGNATURE_XML)
                } else {
                    tvDetail.text = intent.getStringExtra(ORIGINAL_XML)
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {

            }

            override fun onTabReselected(tab: TabLayout.Tab) {

            }
        })

    }

}