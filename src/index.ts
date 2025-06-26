import { 
  HolisticAnalyzerAgent, 
  FeatureExtractorAgent, 
  FeatureDocumentationAgent, 
  DeveloperAgent,
  AnalysisScope 
} from '@hai/agent-pool';
import { fileToolsList, terminalTool } from '@hai/agent-tools';
import { AnthropicModels, LLMPlatforms } from '@hai/agent-core';
import { Langfuse } from "langfuse";
import { v4 as uuidv4 } from "uuid";
import { config } from 'dotenv';
import path from 'path';
import fs from 'fs';

// Load environment variables from .env file
config();

// Initialize Langfuse for tracing
const langfuse = new Langfuse({
  secretKey: process.env.LANGFUSE_SECRET_KEY!,
  publicKey: process.env.LANGFUSE_PUBLIC_KEY!,
  baseUrl: process.env.LANGFUSE_BASE_URL!
});

// Configuration
const CONFIG = {
  // Legacy application paths
  legacyAppPath: path.resolve(__dirname, '..'),
  legacyJavaSourcePath: path.resolve(__dirname, '../src/net/jforum'), // Legacy Java source files
  
  // Files/folders that are part of the HAI modernization framework
  frameworkFiles: [
    'package-lock.json',
    'package.json', 
    'tsconfig.json',
    'env.example',
    '.env',
    '.gitignore',
    'src/index.ts',
    'node_modules',
    'dist' // Build output directory
  ],
  
  // Output paths
  analysisOutputPath: path.resolve(__dirname, '../modernization-output/analysis'),
  featuresOutputPath: path.resolve(__dirname, '../modernization-output/features'),
  documentationOutputPath: path.resolve(__dirname, '../modernization-output/documentation'),
  modernizedOutputPath: path.resolve(__dirname, '../modernization-output/spring-boot-app'),
  
  // Session tracking
  sessionId: uuidv4(),
  
  // JForum2 specific database tables (based on typical JForum structure)
  dbTables: [
    'jforum_users', 'jforum_groups', 'jforum_categories', 'jforum_forums',
    'jforum_topics', 'jforum_posts', 'jforum_posts_text', 'jforum_attachments',
    'jforum_privmsgs', 'jforum_privmsgs_text', 'jforum_moderator_groups',
    'jforum_user_groups', 'jforum_sessions', 'jforum_smilies', 'jforum_words',
    'jforum_karma', 'jforum_bookmarks', 'jforum_quota_limit', 'jforum_extension_groups'
  ]
};

async function createOutputDirectories() {
  const dirs = [
    CONFIG.analysisOutputPath,
    CONFIG.featuresOutputPath,
    CONFIG.documentationOutputPath,
    CONFIG.modernizedOutputPath
  ];
  
  for (const dir of dirs) {
    if (!fs.existsSync(dir)) {
      fs.mkdirSync(dir, { recursive: true });
    }
  }
}

async function getJavaFiles(): Promise<string[]> {
  const javaFiles: string[] = [];
  
  function scanDirectory(dir: string) {
    const items = fs.readdirSync(dir);
    
    for (const item of items) {
      const fullPath = path.join(dir, item);
      const stat = fs.statSync(fullPath);
      
      // Skip framework files and directories
      if (CONFIG.frameworkFiles.includes(item) || 
          item.startsWith('.') || 
          item === 'node_modules' ||
          item === 'dist') {
        continue;
      }
      
      if (stat.isDirectory()) {
        scanDirectory(fullPath);
      } else if (stat.isFile() && item.endsWith('.java')) {
        javaFiles.push(fullPath);
      }
    }
  }
  
  // Scan the legacy Java source directories 
  // Note: src/net/jforum contains the legacy JForum2 Java code
  const javaScanPaths = [
    CONFIG.legacyJavaSourcePath, // Main Java source path: src/net/jforum
    path.resolve(__dirname, '../tools/phpbb2jforum/src'), // Additional tools Java code
    path.resolve(__dirname, '../tests/core') // Test Java code
  ];
  
  for (const scanPath of javaScanPaths) {
    if (fs.existsSync(scanPath)) {
      scanDirectory(scanPath);
    }
  }
  
  return javaFiles;
}

async function step1_holisticAnalysis(): Promise<void> {
  console.log("\n🔍 Step 1: Performing Holistic Analysis of Legacy JForum2 Application");
  
  try {
    // Check if analysis already exists
    const folderAnalysisFile = path.join(CONFIG.analysisOutputPath, "holistic-analysis", "folder-structure-analysis.md");
    const architectureAnalysisFile = path.join(CONFIG.analysisOutputPath, "holistic-analysis", "architectural-patterns-analysis.md");
    
    // Check each analysis independently
    const folderAnalysisExists = fs.existsSync(folderAnalysisFile);
    const architectureAnalysisExists = fs.existsSync(architectureAnalysisFile);
    
    if (folderAnalysisExists && architectureAnalysisExists) {
      console.log("✅ Step 1 analysis files already exist, skipping...");
      console.log("- Folder analysis:", folderAnalysisFile);
      console.log("- Architecture analysis:", architectureAnalysisFile);
      return;
    }
    
    // First analyze folder structure (only if it doesn't exist)
    if (!folderAnalysisExists) {
      console.log("📁 Analyzing folder structure...");
      const folderAnalyzer = new HolisticAnalyzerAgent({
        model: AnthropicModels.CLAUDE_3_7_SONNET,
        platform: LLMPlatforms.Default,
        temperature: 0.3,
        tools: [...fileToolsList, terminalTool],
        // Remove agentEventHandler completely for non-interactive mode
        logMetaData: {
          sessionId: CONFIG.sessionId,
          tags: ["holistic-analysis", "folder-structure", "jforum2-modernization", "step1"]
        },
        context: {
          projectPath: CONFIG.legacyAppPath,
          outputPath: CONFIG.analysisOutputPath,
          analysisScope: AnalysisScope.FOLDER_STRUCTURE
        },
        customInstructions: `Analyze ONLY the legacy JForum2 application folder structure. IGNORE these HAI modernization framework files:
        ${CONFIG.frameworkFiles.join(', ')}
        
        Focus on the legacy application structure:
        - src/net/jforum/ (legacy Java source)
        - WEB-INF/, templates/, www/, tools/, tests/, lib/ (legacy application components)
        - build.xml, .project, .classpath (legacy build configuration)
        
        Provide a comprehensive folder structure analysis report and finish automatically without asking for user input.`,
        conversationHistoryPath: path.join(CONFIG.analysisOutputPath, "folder_structure_history.json")
      });
      
      await folderAnalyzer.execute();
    } else {
      console.log("✅ Folder structure analysis already exists, skipping...");
    }
    
    // Then perform comprehensive architecture analysis (only if it doesn't exist)
    if (!architectureAnalysisExists) {
      console.log("🏗️ Analyzing architecture...");
      const architectureAnalyzer = new HolisticAnalyzerAgent({
        model: AnthropicModels.CLAUDE_3_7_SONNET,
        platform: LLMPlatforms.Default,
        temperature: 0.3,
        tools: [...fileToolsList, terminalTool],
        // Remove agentEventHandler completely for non-interactive mode
        logMetaData: {
          sessionId: CONFIG.sessionId,
          tags: ["holistic-analysis", "architecture", "jforum2-modernization", "step1"]
        },
        context: {
          projectPath: CONFIG.legacyAppPath,
          outputPath: CONFIG.analysisOutputPath,
          analysisScope: AnalysisScope.ARCHITECTURE
        },
        customInstructions: `CRITICAL: First, read the folder-structure-analysis report at outputPath/holistic-analysis/ folder before taking any action. 
        
        Focus ONLY on the legacy JForum2 application files. IGNORE these modernization framework files:
        ${CONFIG.frameworkFiles.join(', ')}
        
        The legacy application consists of:
        - src/net/jforum/ (Java source code)
        - WEB-INF/ (web configuration)
        - templates/ (JSP/HTML templates)
        - www/ (web assets)
        - tools/ (build tools)
        - tests/ (legacy tests)
        - lib/ (Java libraries)
        - build.xml, .project, .classpath (Eclipse/Ant build files)
        - Various JSP, HTML, and configuration files
        
        Analyze the architecture and identify MVC patterns, servlet configurations, and database access patterns typical in legacy JForum applications. Provide a comprehensive architecture analysis report and finish automatically without asking for user input.`,
        conversationHistoryPath: path.join(CONFIG.analysisOutputPath, "architecture_history.json")
      });
      
      await architectureAnalyzer.execute();
    } else {
      console.log("✅ Architecture analysis already exists, skipping...");
    }
    
    console.log("✅ Step 1 completed: Holistic analysis reports generated");
    
  } catch (error) {
    console.error("❌ Step 1 failed:", error);
    throw error;
  }
}

async function step2_featureExtraction(): Promise<void> {
  console.log("\n🔧 Step 2: Extracting Features from Legacy JForum2 Application");
  
  try {
    // Check if feature extraction already exists
    const featuresFile = path.join(CONFIG.featuresOutputPath, "extractedFeatures.json");
    
    if (fs.existsSync(featuresFile)) {
      console.log("✅ Step 2 feature extraction already completed, skipping...");
      console.log("- Features file:", featuresFile);
      return;
    }
    
    const javaFiles = await getJavaFiles();
    console.log(`Found ${javaFiles.length} Java files to analyze`);
    
    if (javaFiles.length === 0) {
      throw new Error("No Java files found in the project");
    }
    
    console.log("⚙️ Extracting features from Java files...");
    const featureExtractor = new FeatureExtractorAgent({
      model: AnthropicModels.CLAUDE_3_7_SONNET,
      platform: LLMPlatforms.Default,
      temperature: 0.3,
      maxWorkers: 5, // basic value lowered for aws bedrock constraints. default is 75
      logMetaData: {
        sessionId: CONFIG.sessionId,
        tags: ["feature-extraction", "jforum2-modernization", "step2"]
      },
      context: {
        projectPath: CONFIG.legacyAppPath,
        outputDir: CONFIG.featuresOutputPath,
      }
    });
    
    await featureExtractor.execute({
      filesToProcess: javaFiles,
      dbTables: CONFIG.dbTables
    });
    
    console.log("✅ Step 2 completed: Feature extraction completed");
    
  } catch (error) {
    console.error("❌ Step 2 failed:", error);
    throw error;
  }
}

async function step3_manualReview(): Promise<void> {
  console.log("\n📋 Step 3: Manual Review Required");
  console.log("Please review the extracted features in:", CONFIG.featuresOutputPath);
  console.log("Key files to review:");
  console.log("- extractedFeatures.json: Contains all extracted features");
  console.log("- Review and identify high-level features for documentation");
  console.log("\nPress Enter when you have completed the review and identified high-level features...");
  
  // Wait for user input with proper stdin handling
  return new Promise((resolve) => {
    const readline = require('readline');
    const rl = readline.createInterface({
      input: process.stdin,
      output: process.stdout
    });
    
    rl.question('', () => {
      rl.close();
      console.log("✅ Step 3 completed: Manual review acknowledged");
      resolve(undefined);
    });
  });
}

async function step4_featureDocumentation(): Promise<void> {
  console.log("\n📚 Step 4: Creating Comprehensive Feature Documentation");
  
  try {
    // Check if documentation already exists
    const docFiles = fs.readdirSync(CONFIG.documentationOutputPath).filter(f => f.endsWith('.md'));
    
    if (docFiles.length > 0) {
      console.log("✅ Step 4 documentation already exists, skipping...");
      console.log("- Documentation files:", docFiles.join(', '));
      return;
    }
    
    const documentationPrompt = `
    Based on the feature extraction results from ${CONFIG.featuresOutputPath}, create comprehensive documentation for the high-level JForum2 features including:
    
    1. User Management System (registration, authentication, profiles)
    2. Forum Structure Management (categories, forums, topics)
    3. Post and Reply System (creating, editing, managing posts)
    4. Private Messaging System
    5. Moderation and Administration Features
    6. Search and Navigation Features
    7. Attachment and File Management
    8. User Karma and Reputation System
    
    For each feature, provide:
    - Detailed business logic and workflow
    - API endpoints and data structures needed for Spring Boot implementation
    - Database schema requirements
    - Security considerations
    - Integration points with other features
    
    This documentation will be used as input for modernizing the application to Spring Boot.
    Finish automatically without asking for user input.
    `;
    
    console.log("📝 Creating feature documentation...");
    const featureDocumenter = new FeatureDocumentationAgent({
    model: AnthropicModels.CLAUDE_3_7_SONNET,
    platform: LLMPlatforms.Default,
    temperature: 0.7,
    tools: [...fileToolsList, terminalTool],
      // Remove agentEventHandler for non-interactive mode
      logMetaData: {
        sessionId: CONFIG.sessionId,
        tags: ["feature-documentation", "jforum2-modernization", "step4"]
      },
      context: {
        projectPath: CONFIG.legacyAppPath,
        outputPath: CONFIG.documentationOutputPath,
      },
      conversationHistoryPath: path.join(CONFIG.documentationOutputPath, "documentation_history.json")
    });
    
    await featureDocumenter.execute({
      humanPrompt: documentationPrompt
    });
    
    console.log("✅ Step 4 completed: Feature documentation generated");
    
  } catch (error) {
    console.error("❌ Step 4 failed:", error);
    throw error;
  }
}

async function step5_createModernBoilerplate(): Promise<void> {
  console.log("\n🏗️ Step 5: Creating Modern Spring Boot Boilerplate");
  
  try {
    // Check if boilerplate already exists
    const pomFile = path.join(CONFIG.modernizedOutputPath, "pom.xml");
    const srcMain = path.join(CONFIG.modernizedOutputPath, "src", "main");
    
    if (fs.existsSync(pomFile) && fs.existsSync(srcMain)) {
      console.log("✅ Step 5 Spring Boot boilerplate already exists, skipping...");
      console.log("- Maven file:", pomFile);
      console.log("- Source directory:", srcMain);
      return;
    }
    
    const boilerplatePrompt = `
    Create a modern Spring Boot boilerplate application for the JForum2 modernization project with the following requirements:
    
    1. **Project Structure:**
       - Maven-based Spring Boot 3.x project
       - Proper package structure: controller, service, repository, entity, dto, config, security
       - Application properties with profiles (dev, test, prod)
    
    2. **Dependencies:** Include essential Spring Boot starters:
       - spring-boot-starter-web
       - spring-boot-starter-data-jpa
       - spring-boot-starter-security
       - spring-boot-starter-validation
       - spring-boot-starter-test
       - spring-boot-starter-actuator
       - Database drivers (H2 for dev, MySQL/PostgreSQL for prod)
       - JWT token library
    
    3. **Core Configuration:**
       - Security configuration with JWT authentication
       - JPA configuration with auditing
       - CORS configuration
       - Exception handling configuration
       - Swagger/OpenAPI documentation setup
    
    4. **Base Entities:** Create foundational JPA entities based on JForum2 schema:
       - User, Group, Category, Forum, Topic, Post
       - Include audit fields (createdDate, modifiedDate, createdBy, modifiedBy)
    
    5. **Infrastructure:**
       - Base repository interfaces
       - Base service classes
       - Common DTOs and response wrappers
       - Validation groups and custom validators
    
    6. **Testing Setup:**
       - Test configuration with test containers
       - Base test classes for integration tests
       - Mock configurations
    
    7. **Docker Configuration:**
       - Dockerfile for application
       - docker-compose.yml for development environment
    
    8. **Documentation:**
       - README.md with setup instructions
       - API documentation structure
    
    Create all necessary files and folders with proper Spring Boot conventions.
    Finish automatically without asking for user input.
    `;
    
    console.log("🚧 Creating Spring Boot boilerplate...");
    const boilerplateAgent = new DeveloperAgent({
      model: AnthropicModels.CLAUDE_3_7_SONNET,
      platform: LLMPlatforms.Default,
      temperature: 0.5,
      tools: [...fileToolsList, terminalTool],
      // Remove agentEventHandler for non-interactive mode
      customInstructions: `
      Follow Spring Boot best practices including:
      - Proper project structure with layered architecture
      - Configuration management with application.yml
      - JPA entities with proper relationships
      - RESTful API design principles
      - Security configuration with Spring Security
      - Exception handling and validation
      - Testing framework setup
      - Docker configuration for deployment
      `,
      logMetaData: {
        sessionId: CONFIG.sessionId,
        tags: ["boilerplate-creation", "spring-boot", "jforum2-modernization", "step5"]
      },
      context: {
        modernizedPath: CONFIG.modernizedOutputPath,
        holisticAnalysisPath: CONFIG.analysisOutputPath
      },
      conversationHistoryPath: path.join(CONFIG.modernizedOutputPath, "boilerplate_history.json")
    });
    
    await boilerplateAgent.execute({
      humanPrompt: boilerplatePrompt
    });
    
    console.log("✅ Step 5 completed: Spring Boot boilerplate created");
    
  } catch (error) {
    console.error("❌ Step 5 failed:", error);
    throw error;
  }
}

async function step6_generateModernCode(): Promise<void> {
  console.log("\n⚡ Step 6: Generating Modern Spring Boot Implementation");
  
  try {
    // Check if comprehensive JForum2 implementation already exists
    // Look for specific JForum2 feature controllers/services, not just any Java files
    const srcMainJava = path.join(CONFIG.modernizedOutputPath, "src", "main", "java");
    let hasFullImplementation = false;
    
    if (fs.existsSync(srcMainJava)) {
      // Check for specific JForum2 implementation files (not just boilerplate)
      const requiredJForumFiles = [
        'UserController', 'ForumController', 'PostController', 'TopicController',
        'UserService', 'ForumService', 'PostService', 'TopicService',
        'UserRepository', 'ForumRepository', 'PostRepository', 'TopicRepository'
      ];
      
      const allJavaFiles: string[] = [];
      function collectJavaFiles(dir: string): void {
        if (fs.existsSync(dir)) {
          const items = fs.readdirSync(dir, { withFileTypes: true });
          for (const item of items) {
            if (item.isDirectory()) {
              collectJavaFiles(path.join(dir, item.name));
            } else if (item.name.endsWith('.java')) {
              allJavaFiles.push(item.name);
            }
          }
        }
      }
      collectJavaFiles(srcMainJava);
      
      // Check if we have a substantial JForum2 implementation (at least 8 of the required files)
      const foundFiles = requiredJForumFiles.filter(required => 
        allJavaFiles.some(file => file.includes(required))
      );
      
      hasFullImplementation = foundFiles.length >= 8; // Require at least 8 core JForum2 files
      
      if (hasFullImplementation) {
        console.log("✅ Step 6 comprehensive JForum2 implementation already exists, skipping...");
        console.log(`- Found ${foundFiles.length}/${requiredJForumFiles.length} required JForum2 files`);
        console.log("- Implementation directory:", srcMainJava);
        return;
      } else if (allJavaFiles.length > 0) {
        console.log(`📝 Found ${allJavaFiles.length} Java files, but only ${foundFiles.length}/${requiredJForumFiles.length} JForum2-specific files`);
        console.log("- Proceeding with full implementation...");
      }
    }
    
    const implementationPrompt = `
    Using the feature documentation from ${CONFIG.documentationOutputPath} and the analysis from ${CONFIG.analysisOutputPath}, implement the complete JForum2 modernization in Spring Boot.
    
    **Implementation Requirements:**
    
    1. **User Management Module:**
       - User registration and authentication with JWT
       - Profile management and preferences
       - User groups and permissions
       - Password reset and email verification
    
    2. **Forum Structure Module:**
       - Category and forum management
       - Topic creation and management
       - Hierarchical permissions and access control
    
    3. **Post Management Module:**
       - Post creation, editing, and deletion
       - Reply threading and post history
       - Rich text content support
       - Post moderation features
    
    4. **Private Messaging System:**
       - Send and receive private messages
       - Message threads and organization
       - Notification system
    
    5. **Search and Navigation:**
       - Full-text search across posts and topics
       - Advanced search filters
       - Pagination and sorting
    
    6. **Administration Features:**
       - Admin dashboard and controls
       - User and content moderation
       - System configuration and monitoring
    
    7. **API Endpoints:** Create comprehensive REST APIs for all features with:
       - Proper request/response DTOs
       - Input validation and sanitization
       - Error handling and status codes
       - API documentation with Swagger
    
    8. **Database Layer:**
       - JPA entities with proper relationships
       - Efficient repository implementations
       - Database migrations with Flyway
       - Query optimization
    
    9. **Security Implementation:**
       - JWT-based authentication
       - Role-based access control
       - CSRF protection
       - Input validation and XSS prevention
    
    10. **Testing:**
        - Unit tests for services and controllers
        - Integration tests for API endpoints
        - Repository tests with test containers
        - Security tests
    
    Build upon the boilerplate code created in Step 5 and implement all the features identified in the legacy JForum2 application.
    Finish automatically without asking for user input.
    `;
    
    console.log("⚡ Generating Spring Boot implementation...");
    const implementationAgent = new DeveloperAgent({
      model: AnthropicModels.CLAUDE_3_7_SONNET,
      platform: LLMPlatforms.Default,
      temperature: 0.6,
      tools: [...fileToolsList, terminalTool],
      // Remove agentEventHandler for non-interactive mode
      customInstructions: `
      Implement the complete JForum2 modernization following these principles:
      - Clean Architecture with clear separation of concerns
      - RESTful API design with proper HTTP status codes
      - Comprehensive validation and error handling
      - Security best practices with JWT authentication
      - Efficient database queries with JPA
      - Unit and integration tests for all components
      - Proper logging and monitoring
      `,
      logMetaData: {
        sessionId: CONFIG.sessionId,
        tags: ["implementation", "spring-boot", "jforum2-modernization", "step6"]
      },
    context: {
        modernizedPath: CONFIG.modernizedOutputPath,
        holisticAnalysisPath: CONFIG.analysisOutputPath,
        sourcePath: CONFIG.legacyAppPath
      },
      conversationHistoryPath: path.join(CONFIG.modernizedOutputPath, "implementation_history.json")
    });
    
    await implementationAgent.execute({
      humanPrompt: implementationPrompt
    });
    
    console.log("✅ Step 6 completed: Modern Spring Boot implementation generated");
    
  } catch (error) {
    console.error("❌ Step 6 failed:", error);
    throw error;
  }
}

async function runModernizationWorkflow(): Promise<void> {
  console.log("🚀 Starting JForum2 Modernization Workflow");
  console.log("Session ID:", CONFIG.sessionId);
  
  const workflowTrace = langfuse.trace({
    name: "jforum2-modernization-complete-workflow",
    sessionId: CONFIG.sessionId,
    userId: process.env.LANGFUSE_USER_ID,
    metadata: { 
      project: "jforum2-modernization",
      targetFramework: "Spring Boot",
      steps: 6
    }
  });
  
  try {
    // Create output directories
    await createOutputDirectories();
    
    // Execute all steps in sequence
    await step1_holisticAnalysis();
    await step2_featureExtraction();
    await step3_manualReview();
    await step4_featureDocumentation();
    await step5_createModernBoilerplate();
    await step6_generateModernCode();
    
    workflowTrace.update({ 
      output: { 
        status: "completed",
        analysisPath: CONFIG.analysisOutputPath,
        featuresPath: CONFIG.featuresOutputPath,
        documentationPath: CONFIG.documentationOutputPath,
        modernizedAppPath: CONFIG.modernizedOutputPath
      } 
    });
    
    console.log("\n🎉 JForum2 Modernization Workflow Completed Successfully!");
    console.log("\n📁 Output Locations:");
    console.log("- Analysis Reports:", CONFIG.analysisOutputPath);
    console.log("- Extracted Features:", CONFIG.featuresOutputPath);
    console.log("- Feature Documentation:", CONFIG.documentationOutputPath);
    console.log("- Modernized Spring Boot App:", CONFIG.modernizedOutputPath);
    console.log("\n✨ Your legacy JForum2 application has been successfully modernized to Spring Boot!");
    
  } catch (error) {
    workflowTrace.update({ output: { status: "failed", error: String(error) } });
    console.error("\n❌ Modernization workflow failed:", error);
    throw error;
  } finally {
    // Ensure Langfuse traces are flushed
    await langfuse.flushAsync();
  }
}

async function main() {
  try {
    await runModernizationWorkflow();
  } catch (error) {
    console.error("Fatal error in modernization workflow:", error);
    process.exit(1);
  }
}

main().catch(console.error);