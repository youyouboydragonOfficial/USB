package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun HelpScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianDark)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "❓ USB容量偽装検証ガイド & 仕組み",
            color = CyberCyan,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )

        HelpCard(
            title = "🚨 偽装USBメモリ (Fake Capacity) とは？",
            description = """
                安価な16GBや32GBのUSBフラッシュメモリやSDカードの制御チップ（コントローラーファームウェア）を不正に書き換え、PCやAndroid端末上で「1TB」や「2TB」と偽装表示させる詐欺製品です。
                
                実際の物理メモリ容量を超えてデータを書き込もうとすると、コントローラーは古いデータを無言で上書き・消去するため、後からファイルを開いた時に重大なデータ破損（写真・動画・書類が開けない等）が発生します。
            """.trimIndent()
        )

        HelpCard(
            title = "🛡️ 本アプリ (USB Guard) の検証原理",
            description = """
                1. 【書き込みフェーズ】
                独自の決定論的疑似乱数シード (Deterministic PRNG) を用いたバイナリデータブロック (例: 16MB) を作成し、USBメモリ内に順番に保存します。
                
                2. 【読み出し＆整合性検査フェーズ】
                書き込まれたファイルを全件読み出し、バイト単位で期待されるパターンと一致するかチェックします。
                
                上書きループやデータの消失が発生した場合、即座に破損ブロック（Corrupt Block）として検出し、安全に使用できる「真の実効容量」を診断します。
            """.trimIndent()
        )

        HelpCard(
            title = "🔌 USB OTG & Storage Access Framework (SAF)",
            description = """
                Android 10以降ではセキュリティが強化され、USBメモリへの読み書きには「Storage Access Framework (SAF)」が使用されます。
                
                [USBメモリを選択] ボタンを押すとOS標準のフォルダ選択画面が開きます。接続されたUSBメモリのルートまたはテスト用フォルダを選択し、アクセス許可を与えてください。
            """.trimIndent()
        )

        HelpCard(
            title = "💡 推奨テスト手順",
            description = """
                ・まず Quick (256MB) または Standard (1GB) テストで転送速度と動作を確認してください。
                ・全容検査を行う場合は Extreme (16GB) または Custom でメモリ全体のサイズを指定してテストを実行します。
                ・テスト完了後は自動クリーンアップ機能でダミーファイルを消去できます。
            """.trimIndent()
        )

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
private fun HelpCard(title: String, description: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardSurfaceDark),
        border = BorderStroke(1.dp, CardBorderDark),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(title, color = CyberCyan, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            Text(description, color = TextPrimaryDark, fontSize = 12.sp, lineHeight = 18.sp)
        }
    }
}
