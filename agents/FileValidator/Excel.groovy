@Grab('org.apache.poi:poi:5.2.4')
@Grab('org.apache.poi:poi-ooxml:5.2.4')

import org.apache.poi.ss.usermodel.*

def validateExcelFile(String filePath) {
    def validationResults = [
        isValid: false,
        fileExists: false,
        correctExtension: false,
        readable: false,
        hasSheets: false,
        errors: []
    ]
    
    try {
        // Check file existence
        def file = new File(filePath)
        validationResults.fileExists = file.exists()
        if (!file.exists()) {
            validationResults.errors << "File does not exist"
            return validationResults
        }
        
        // Check extension
        def extension = filePath.toLowerCase()
        validationResults.correctExtension = extension.endsWith('.xlsx') || extension.endsWith('.xls')
        if (!validationResults.correctExtension) {
            validationResults.errors << "Invalid file extension"
            return validationResults
        }
        
        // Try to open and read
        def workbook = WorkbookFactory.create(file)
        validationResults.readable = true
        
        // Check for sheets
        validationResults.hasSheets = workbook.numberOfSheets > 0
        if (!validationResults.hasSheets) {
            validationResults.errors << "No sheets found"
        }
        
        // Test reading data from first sheet
        if (workbook.numberOfSheets > 0) {
            def sheet = workbook.getSheetAt(0)
            sheet.physicalNumberOfRows // Will throw if corrupted
        }
        
        workbook.close()
        validationResults.isValid = validationResults.fileExists && 
                                  validationResults.correctExtension && 
                                  validationResults.readable && 
                                  validationResults.hasSheets
        
    } catch (Exception e) {
        validationResults.errors << "Corruption or read error: ${e.message}"
    }
    
    return validationResults
}
