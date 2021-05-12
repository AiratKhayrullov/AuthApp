package mlita.authapp.activities

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import mlita.authapp.database.AuthDatabase
import mlita.authapp.database.User
import mlita.authapp.databinding.ActivityCreatingAccountBinding
import java.math.BigInteger
import java.time.LocalTime
import kotlin.math.pow

class CreatingAccountActivity: AppCompatActivity() {
    companion object {
        /*  val alphabet = arrayListOf(
      '!', '"', '#', '$', '%', '&', '\'', '(', ')', '*', '+', ',', '-', '.', '.', '/', '0', '1',
      '2', '3', '4', '5', '6', '7', '8', '9', ':', ';', '<', '=', '>', '?', '@', 'A', 'B', 'C', 'D', 'E', 'F',
      'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', '[',
      '\\', ']', '^', '_', '`', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p',
      'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', '{', '|', '}', '~', '♫', '…', '„', '€', '‹', '“', '”',
      '•', '–', '—', '™', '›', '§', '©', 'Є', '«', '¬', '®', '°', '»', '☺', '↨', '↑', '↓', '→', '←', '▲', '▼',
      '►', '◄', '‼', '¶', '○'
  )*/
        val alphabet = arrayListOf(
            'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q',
            'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H',
            'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y',
            'Z', '1', '2', '3', '4', '5', '6', '7', '8', '9', '0', 'а', 'б', 'в', 'г', 'д', 'е',
            'ё', 'ж', 'з', 'и', 'й', 'к', 'л', 'м', 'н', 'о', 'п', 'р', 'с', 'т', 'у', 'ф', 'х',
            'ц', 'ч', 'ш', 'щ', 'ъ', 'ы', 'ь', 'э', 'ю', 'я', 'А', 'Б', 'В', 'Г', 'Д', 'Е', 'Ё',
            'Ж', 'З', 'И', 'Й', 'К', 'Л', 'М', 'Н', 'О', 'П', 'Р', 'С', 'Т', 'У', 'Ф', 'Х', 'Ц',
            'Ч', 'Ш', 'Щ', 'Ъ', 'Ы', 'Ь', 'Э', 'Ю', 'Я'
        )
    }

    private lateinit var login: String // Логин
    private lateinit var password: String // Пароль
    private lateinit var userName: String // Nickname
    private lateinit var binding: ActivityCreatingAccountBinding
    private var errorState: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreatingAccountBinding.inflate(layoutInflater)
        setContentView(binding.root)


        binding.btnCreateAccount.setOnClickListener {
            createAccount()
        }

        binding.btnSignIn.setOnClickListener {
            finish()
        }


    }

    private fun createAccount() {
        val dao = AuthDatabase.getInstance(this).authDao

        userName = binding.etUsername.text.toString()
        login = binding.etLogin.text.toString()
        password = binding.etPassword.text.toString()

        checkInfo(userName, login, password)

        if (errorState) {
            return
        }

        val binaryPassword = convertTextToBinarySequence(password)

        // 3) Генерация случайного слова – соли
        val salt = createSalt()

        // 4) Переводим соль в бинарный вид
        val binarySalt = convertTextToBinarySequence(salt!!)

        // 5) Выбор правила развития клеточного автомата на основе соли
        val binaryRule = createRule(binarySalt!!)

        // 6) Считывание количества раундов шифрования (последний символ соли)
        val numberOfRounds = alphabet.indexOf(salt[salt.length - 1])

        // 7) Создание дополнительного списка развития клеточного автомата,где внесены всевозможные текущие окрестности клетки
        val currentPattern: List<String> =
            listOf("111", "110", "101", "100", "011", "010", "001", "000")

        // 8) Получение списка развития клеточного автомата, где внесены новые состояния клеток с помощью кодов Вольфрама
        val newPattern = convertStringToArrayList(binaryRule!!)


        // 9) Создаём начальное состояние автомата = пароль в бинарном виде + соль в бинарном виде
        val initStateCellAut = binaryPassword + binarySalt

        // 10) Вычисление хэш-кода пароля + соли n раз, где n - количество раундов шифрования
        var binaryHashCode: String? = ""
        for (i in 0 until numberOfRounds) {
            binaryHashCode = createHashCode(initStateCellAut, currentPattern, newPattern!!)
        }

        // 11) Получившееся состояние клеточного автомата переводится в символьное представление
        val hashCode = convertBinarySequenceToText(binaryHashCode!!).toString()


        lifecycleScope.launch {
            dao.insertUser(User(userName = userName, login = login, hashCode = hashCode, salt = salt))
            Toast.makeText(applicationContext, "Account ${userName} was successfully created", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun checkInfo(username: String, login: String, password: String) {

        if (errorState) {
            errorState = false
        }

        if (username.isEmpty()) {
            binding.tvErrorUsername.text = "Username can't be empty"
            errorState = true
            binding.etUsername.setText("", TextView.BufferType.EDITABLE)
        }

        username.forEach {
            if (!alphabet.contains(it)) {
                binding.tvErrorUsername.text = "Username contains unresolved characters"
                errorState = true
                binding.etUsername.setText("", TextView.BufferType.EDITABLE)
            }
        }
        if (!errorState) {
            binding.tvErrorUsername.text = ""
        }

        //---------------------------------------
        if (login.isEmpty()) {
            binding.tvErrorLogin.text = "Login can't be empty"
            errorState = true
            binding.etLogin.setText("", TextView.BufferType.EDITABLE)

        }

        login.forEach {
            if (!alphabet.contains(it)) {
                binding.tvErrorLogin.text = "Login contains unresolved characters"
                errorState = true
                binding.etLogin.setText("", TextView.BufferType.EDITABLE)

            }
        }
        if (!errorState) {
            binding.tvErrorLogin.text = ""
        }
        //----------------------------------------
        if (password.length < 6 || password.length > 32) {
            binding.tvErrorPassword.text = "Password length must be 6-32 characters"
            errorState = true
            binding.etPassword.setText("", TextView.BufferType.EDITABLE)
        }
        password.forEach {
            if (!alphabet.contains(it)) {
                binding.tvErrorPassword.text = "Password contains unresolved characters"
                errorState = true
                binding.etPassword.setText("", TextView.BufferType.EDITABLE)
            }
        }

        if (!errorState) {
            binding.tvErrorPassword.text = ""
        }
    }


    /**
     * Метод создания хэш-кода. Идея в том, что берём окрестность точки( neighborhoodOfPoint), находим её(окрестность)
     * в currentPattern, берём соотвествующий currentPattern новое состояние клетки в newPattern (той самой клетки,
     * которой и смотрели окрестность). Заносим это новое состояние клетки в новое состояние автомата (newStateCellAut).
     * newStateCellAut - новое состояние клеточного автомата
     * neighborhoodOfPoint - окрестность точки, например "101" или "011" и тп.
     *
     * @param currentPattern   Текущее состояние (верхняя строчка кодов Вольфрама - все окрестности, которые только могут быть)
     * @param initStateCellAut Начальное состояние клеточного автомата
     * @param newPattern       Новое состояние центральных клеток
     * @return Хэш-код в бинарном представлении
     */
    private fun createHashCode(initStateCellAut: String, currentPattern: List<String>, newPattern: ArrayList<Char>): String {
        val newStateCellAut = StringBuilder()
        var neighborhoodOfPoint = ""

        // С помощью цикла мы можем обработать почти все окрестности, кроме первой и последней точек(иначе выйдем заграницу)
        for (i in 0 until initStateCellAut.length - 2) {
            // Берём окрестность рассматриваемой точки
            neighborhoodOfPoint = initStateCellAut.substring(i, i + 3)

            // Здесь смотрим этого соседа(neighborhoodOfPoint) в currentPattern. Далее берём новое состояние,
            // которое относилось к определенной currentPattern и заносим его в newStateCellAut
            newStateCellAut.append(newPattern[currentPattern.indexOf(neighborhoodOfPoint)])
        }

        // Чтобы обработать последний и первый бит: как бы заворачиваем нашу последовательность в кольцо
        // Например: "1101010". Мы обработали "11010", но не обработали конец 0 и начало 1 (см дальше)

        // Связываем препоследний и последний бит с первым битом (как кольцо), тем самым обрабатываем последний бит.
        // Если брать пример "1101010", то здесь neighborhoodOfPoint = "101"
        neighborhoodOfPoint =
            initStateCellAut.substring(initStateCellAut.length - 2) + initStateCellAut[0]
        newStateCellAut.append(newPattern[currentPattern.indexOf(neighborhoodOfPoint)])

        // Связываем последний бит с первым и вторым битом, тем самым обрабатываем первый бит
        // Если брать пример "1101010", то здесь neighborhoodOfPoint = "011"
        neighborhoodOfPoint =
            initStateCellAut.substring(initStateCellAut.length - 1) + initStateCellAut[0] + initStateCellAut[1]
        newStateCellAut.append(newPattern[currentPattern.indexOf(neighborhoodOfPoint)])
        return newStateCellAut.toString()
    }

    /**
     * Метод создания соли.
     * salt - наша соль.
     * Создаем с помощью рандомных чисел и нашего алфавита: alphabet[randomNumber] == есть какой-то символ;
     * По нашему условию длина соли должна быть (32 - длина пароля) (Напоминание: длина пароля не больше чем 32)
     *
     * @return Соль (String)
     */
    private fun createSalt(): String {
        val salt = StringBuilder("")
        var randomNumber = 0
        for (i in 0 until 32 - password!!.length) {
            randomNumber = generateRandomNumber().toInt()
            salt.append(alphabet[randomNumber])
        }
        return salt.toString()
    }


    /**
     * Метод для создания правила развития.
     * Соль в двоичном виде переводится в десятичное число , остаток от деления на 255 которого
     * выступает в роли номера правила развития клеточного автомата (нумерация правил идет от 0 до 255)
     *
     * @param salt Соль в бинарном представлении
     * @return Правило развития в двоичном представлении
     */
    private fun createRule(salt: String): String? {
        val loginFromBinaryLogin = BigInteger(convertBinaryNumberToDecimalNumber(salt))
        // System.out.println("Login from binary login: " + loginFromBinaryLogin);
        val ruleDecimalNumber = loginFromBinaryLogin.remainder(BigInteger.valueOf(255))
        println("Ваш номер правила развития: $ruleDecimalNumber")

        // Нам нужно получить правило в бинарной записи через 8 битов (от 0 до 255 = 8 битов)
        val ruleBinaryNumber = StringBuilder(ruleDecimalNumber.toString(2))
        while (ruleBinaryNumber.length != 8) {
            ruleBinaryNumber.insert(0, "0")
        }
        return ruleBinaryNumber.toString()
    }

    /**
     * Метод генерации псевдослучайного числа;
     * Алгоритм на основе клеточных автоматов;
     * Работа данного алгоритма рассматривается отдельно, здесь лишь практическое применение;
     *
     * @return Псевдослучайное число
     */
    private fun generateRandomNumber(): Long {
        val automat = Array(64) {
            IntArray(
                51
            )
        }
        val now = LocalTime.now()
        var check = 0
        val hour = now.hour
        val bits = Integer.toBinaryString(hour)
        for (i in 0..50) {
            if (check != bits.length) {
                automat[0][i] = Character.getNumericValue(bits[check++])
            } else break
        }
        for (i in 1 until automat.size)  //генерация всех последующих поколений по правилу
            for (j in 0 until automat[i].size) {
                val first: Int = (j - 1 + automat[i].size) % automat[i].size
                val second: Int = (j + 1 + automat[i].size) % automat[i].size
                automat[i][j] =
                    automat[i - 1][first] xor (automat[i - 1][j] or automat[i - 1][second])
            }
        val lenghtNumber = 6 //рандомная длина двоичного кода числа
        var startNumber =
            (Math.random() * (51 - lenghtNumber)).toInt() //рандомная точка старта в столбце
        val number = IntArray(lenghtNumber + 1)
        for (i in 0 until lenghtNumber + 1) {
            number[i] = automat[25][startNumber++]
        }
        var newNumber = ""
        for (i in number.indices) newNumber += Integer.toString(number[i]) //образуем строку для удобного перевода
        return newNumber.toLong(2)
    }


    /**
     * Метод разделения строки на список символов
     * Например: "Liza" -> ['L', 'i', 'z', 'a']
     *
     * @param string Строка для разделения
     * @return Cписок символов строки string
     */
    private fun convertStringToArrayList(string: String): ArrayList<Char>? {
        val arrayList = ArrayList<Char>()
        for (element in string) {
            arrayList.add(element)
        }
        return arrayList
    }

    /**
     * Метод для перевода двоичного числа в десятичное.
     *
     * @param binaryNumber Двоичное число (String)
     * @return Десятичное число (String)
     */
    private fun convertBinaryNumberToDecimalNumber(binaryNumber: String): String {
        var decimalNumber = BigInteger.valueOf(0L)
        var number: Long = 0 // временная переменная
        for (i in binaryNumber.indices) {
            // Здесь как мы и обычно переводим, например из числа 1010 будет  (1·2^3) или (0·2^2) или (1·2^1) или (0·2^0)
            // Счёт здесь слева направо, то есть степень уменьшается
            number = (Character.getNumericValue(binaryNumber[i]) * 2.toDouble()
                .pow((binaryNumber.length - i - 1).toDouble())).toLong()
            // Складываем в общую сумму, например 1010 = (1·2^3)+(0·2^2)+(1·2^1)+(0·2^0) = 10
            decimalNumber = decimalNumber.add(BigInteger.valueOf(number))
        }
        return decimalNumber.toString()
    }

    /**
     * Метод конвертации информации из двоичного представления в символьное представление, используя alphabet
     *
     * @param binarySequence информация в двоичном виде
     * @return Информация в символьное представлении
     */
    private fun convertBinarySequenceToText(binarySequence: String): String? {
        val text = StringBuilder()
        var letter = ""
        var i = 0
        while (i < binarySequence.length) {
            letter = convertBinaryNumberToDecimalNumber(binarySequence.substring(i, i + 7))
            text.append(alphabet[letter.toInt()])
            i += 7
        }
        return text.toString()
    }

    /**
     * Метод для перевода информации из строки символов в двоичную последовательность используя alphabet
     * binaryNumber - Временная переменная, по сути код символова по нашему alphabet. Если длины кода не хватает до 7,
     * то он её дополнит до 7.
     *
     * @param text Информация для перевода
     * @return Переведённая информация в двоичную последовательность
     */
    private fun convertTextToBinarySequence(text: String): String? {
        val binaryInfo = StringBuilder()
        val binaryNumber = StringBuilder()
        for (i in 0 until text.length) {
            // Переводим символ в двоичный код, например 'b' = 1 (по нашей таблице alphabet)
            binaryNumber.append(Integer.toBinaryString(alphabet.indexOf(text[i])))

            // Если длина кода не 7, то дополним до 7, например 'b' = 1 -> 'b' = 0000001
            // Приписываем не значащие нули - слева.
            while (binaryNumber.length != 7) {
                binaryNumber.insert(0, "0")
            }

            // Заносим сюда наше переведенное число, например 0000001
            binaryInfo.append(binaryNumber)

            // Очищаем нашу временную переменную для следующих итераций
            binaryNumber.setLength(0)
        }
        return binaryInfo.toString()
    }

}