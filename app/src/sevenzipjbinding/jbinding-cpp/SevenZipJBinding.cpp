//============================================================================
// Name        : SevenZipBinding.cpp
// Author      :
// Version     :
// Copyright   : Your copyright notice
// Description : Hello World in C++, Ansi-style
//============================================================================


#include <stdlib.h>

#include "SevenZipJBinding.h"

// #include "Common/MyInitGuid.h"

// #include "Windows/DLL.h"


#include "../p7zip/CPP/Common/CommandLineParser.h"
#include "../p7zip/CPP/Common/MyException.h"
#include "../p7zip/CPP/Common/IntToString.h"
#include "../p7zip/CPP/Common/ListFileUtils.h"
#include "../p7zip/CPP/Common/StdInStream.h"
#include "../p7zip/CPP/Common/StdOutStream.h"
#include "../p7zip/CPP/Common/StringConvert.h"
#include "../p7zip/CPP/Common/StringToInt.h"
#include "../p7zip/CPP/Common/Wildcard.h"

#include "../p7zip/CPP/Windows/FileDir.h"
#include "../p7zip/CPP/Windows/FileName.h"
#include "../p7zip/CPP/Windows/Defs.h"

#include "../p7zip/CPP/7zip/IPassword.h"
#include "../p7zip/CPP/7zip/ICoder.h"
#include "../p7zip/CPP/7zip/UI/Common/UpdateAction.h"
#include "../p7zip/CPP/7zip/UI/Common/Update.h"
#include "../p7zip/CPP/7zip/UI/Common/Extract.h"
#include "../p7zip/CPP/7zip/UI/Common/ArchiveCommandLine.h"
#include "../p7zip/CPP/7zip/UI/Common/ExitCode.h"
#ifdef EXTERNAL_CODECS
#include "../Common/LoadCodecs.h"
#endif

#include "../p7zip/CPP/7zip/UI/Console/List.h"
#include "../p7zip/CPP/7zip/UI/Console/OpenCallbackConsole.h"
#include "../p7zip/CPP/7zip/UI/Console/ExtractCallbackConsole.h"
#include "../p7zip/CPP/7zip/UI/Console/UpdateCallbackConsole.h"

#include "../p7zip/CPP/7zip/MyVersion.h"

/*
#if defined( _WIN32) && defined( _7ZIP_LARGE_PAGES)
extern "C"
{
#include "../../../../C/Alloc.h"
}
#endif

#include "myPrivate.h"
#include "Windows/System.h"
*/

/**
 * Fatal error
 */
void fatal(const char * fmt, ...) {
	va_list args;
	va_start(args, fmt);
	fputs("FATAL ERROR: ", stdout);
	vprintf(fmt, args);
	va_end(args);

	fputc('\n', stdout);
	fflush(stdout);

	TRACE_PRINT_OBJECTS

	// exit(-1);

	printf("Crash jvm to get a stack trace\n");
	fflush(stdout);
	int * i = NULL;
	*i = 0;
}

