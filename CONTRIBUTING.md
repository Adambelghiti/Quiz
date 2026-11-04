# Contributing to Quiz Game

Thank you for considering contributing to the Quiz Game project! This guide will help you get started with contributing code, documentation, or reporting issues.

## Getting Started

1. Fork the repository
2. Clone your fork to your local machine:
   ```bash
   git clone https://github.com/your-username/Quizgame.git
   cd Quizgame
   ```
3. Create a new branch for your feature:
   ```bash
   git checkout -b feature/your-feature-name
   ```

## Code Contributions

### Development Setup

Ensure you have the following installed:
- Java 17+
- Node.js 20+
- npm
- Chrome or Edge (for frontend tests)

### Running the Application

1. **Backend**: Run `mvnw.cmd spring-boot:run` in the `Backend` directory
2. **Frontend**: Run `npm install` then `npm start` in the `Frontend` directory

### Making Changes

1. **Create a new branch** for your feature or bug fix
2. **Write clean, well-documented code** following the project's style
3. **Run tests** to ensure your changes don't break existing functionality
4. **Update documentation** if needed (README.md, CONTRIBUTING.md)

### Submitting a Pull Request

1. Commit your changes with clear, descriptive messages
2. Push your branch to your fork:
   ```bash
   git push origin feature/your-feature-name
   ```
3. Open a pull request from your fork to the main repository
4. Include a detailed description of your changes
5. Reference any related issues with `Closes #issue-number`

## Documentation Contributions

We welcome improvements to our documentation! Please follow these guidelines:

1. **Update README.md** with any new setup instructions or features
2. **Update CONTRIBUTING.md** with new contribution guidelines
3. **Add examples** to help new contributors understand how to use the project

## Reporting Issues

If you encounter a bug or have a feature request, please follow these steps:

1. Search existing issues to ensure it hasn't been reported already
2. Create a new issue with the following information:
   - A clear title
   - Detailed description of the problem
   - Steps to reproduce
   - Expected vs actual behavior
   - Environment details (OS, browser, versions)

## Code of Conduct

Please read and follow our [Code of Conduct](CODE_OF_CONDUCT.md) when contributing to this project.

## License

By contributing to this project, you agree that your contributions will be licensed under the MIT License.