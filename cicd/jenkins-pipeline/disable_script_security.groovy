// Jenkins Script Security 禁用脚本（仅用于测试环境）
// 使用方法：管理 Jenkins -> Script Console -> 粘贴此脚本 -> 运行

import jenkins.model.Jenkins
import org.jenkinsci.plugins.scriptsecurity.scripts.ScriptApproval

// 获取 ScriptApproval 实例
def scriptApproval = ScriptApproval.get()

// 方法1：自动批准所有待审批的脚本
scriptApproval.getPendingScripts().each { script ->
    scriptApproval.approveScript(script.hash)
    println "已批准脚本: ${script.hash}"
}

// 方法2：禁用 Script Security 插件（需要重启 Jenkins）
// 注意：这会完全禁用脚本安全检查，仅用于测试环境
def pluginManager = Jenkins.instance.pluginManager
def scriptSecurityPlugin = pluginManager.getPlugin('script-security')
if (scriptSecurityPlugin != null) {
    println "找到 Script Security 插件，版本: ${scriptSecurityPlugin.version}"
    println "警告：完全禁用 Script Security 需要修改配置文件并重启 Jenkins"
    println "建议：使用自动批准方式（方法1）"
}

println "完成！所有待审批脚本已自动批准。"
