import ammonite.ops._
import ammonite.ops.ImplicitWd._
import $ivy.`org.gbz:utils_2.12:0.1.2-SNAPSHOT`, org.gzb.utils.Core._
val crt = %%.keytool ("-printcert", "-sslserver", "calm.dhamma-eu.org", "-rfc").out.lines.mkString("\n")
write.over(pwd/"calm.crt", crt )//.trace
//cp(pwd/up/'calm4/".gitignore", pwd/".gitignore")
//%git 'init
//%git 'status
//%git 'status
