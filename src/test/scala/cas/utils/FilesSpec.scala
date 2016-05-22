package cas.utils

import java.io.File
import java.nio.file.Paths
import org.specs2.mutable.{After, Specification}

class FilesSpec extends Specification with After {
  sequential  // forces all tests to be run sequentially
  val path = Files.resources + "/test"

  "Files" should {
    val c1 ="test\r\ntest"
    val c2 ="test\r\ntest\ntest"
    "create file" in {
      Files.writeToFile(path, "")
      java.nio.file.Files.exists(Paths.get(path)) mustEqual true
    }
    "read from file" in {
      Files.writeToFile(path, c1)
      Files.readFile(path).getOrElse("Nope") mustEqual c1
    }
    "update file" in {
      Files.writeToFile(path, c2)
      val r = Files.readFile(path).getOrElse("Nope")
      r mustNotEqual c1
      r mustEqual c2
    }
    "read nonexistent file return Failure" in {
      Files.readFile(path + "123") must beFailedTry
    }
  }

  override def after = for {
    file <- Option(new File(path))
  } file.deleteOnExit()
}
